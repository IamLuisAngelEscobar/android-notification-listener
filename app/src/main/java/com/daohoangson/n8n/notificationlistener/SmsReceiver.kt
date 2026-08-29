package com.daohoangson.n8n.notificationlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.daohoangson.n8n.notificationlistener.data.database.PendingCapture
import com.daohoangson.n8n.notificationlistener.data.database.PendingCaptureDao
import com.daohoangson.n8n.notificationlistener.data.repository.NotificationRepository
import com.daohoangson.n8n.notificationlistener.fcc.IngestTransform
import com.daohoangson.n8n.notificationlistener.fcc.TransformResult
import com.daohoangson.n8n.notificationlistener.utils.NotificationData
import com.daohoangson.n8n.notificationlistener.work.IngestScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures bank/fintech SMS alerts and routes them into the same fail-safe buffer
 * as the notification listener (ADR-0013). DiDi balance / cashloan spends arrive
 * only by SMS — with no bank push behind them — so without this receiver they are
 * never captured.
 *
 * Reading the SMS itself (not its Google Messages notification) is the whole
 * point: the OS persists and broadcasts every inbound SMS regardless of whether a
 * heads-up notification ever posted, so a silenced / bundled / quickly-dismissed
 * alert is no longer lost. [com.daohoangson.n8n.notificationlistener.work.SmsSweepWorker]
 * backstops this for messages that land while the app is dead.
 *
 * The flow mirrors [NotificationListenerService]: transform on-device, buffer a
 * spend *before* any network call, then kick the drain; a drop from a tracked
 * sender is recorded for miss-rate observability, an untracked sender is ignored.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingCaptureDao: PendingCaptureDao

    @Inject
    lateinit var repository: NotificationRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // A long SMS arrives split across parts that share a sender and time;
        // rejoin the bodies so the parser sees the whole message.
        val sender = messages.first().displayOriginatingAddress ?: return
        val timestamp = messages.first().timestampMillis
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }
        val appContext = context.applicationContext

        // onReceive returns immediately; keep the process alive for the async work.
        val pending = goAsync()
        scope.launch {
            try {
                capture(appContext, sender, body, timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "sms capture failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun capture(appContext: Context, sender: String, body: String, timestamp: Long) {
        when (val result = IngestTransform.transformSms(sender, body, timestamp)) {
            is TransformResult.Dropped -> {
                if (result.fromTrackedSource) {
                    repository.storeUndecidedNotification(
                        payload = body,
                        reason = result.reason,
                        notificationData = NotificationData(
                            packageName = "sms:$sender",
                            title = sender,
                            text = body,
                            timestamp = timestamp,
                            id = 0,
                            tag = null,
                        ),
                    )
                } else {
                    Log.d(TAG, "dropped sms from $sender: ${result.reason}")
                }
            }
            is TransformResult.Ingestable -> {
                val payload = result.payload
                val inserted = pendingCaptureDao.insert(
                    PendingCapture(
                        reference = payload.source.reference,
                        payloadJson = payload.toJson(),
                        account = payload.account,
                    )
                )
                if (inserted == -1L) {
                    Log.d(TAG, "duplicate sms capture ignored: ${payload.source.reference}")
                    return
                }
                IngestScheduler.drainNow(appContext)
            }
        }
    }

    companion object {
        private const val TAG = "FccSmsReceiver"
    }
}
