package com.daohoangson.n8n.notificationlistener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.daohoangson.n8n.notificationlistener.data.database.PendingCapture
import com.daohoangson.n8n.notificationlistener.data.database.PendingCaptureDao
import com.daohoangson.n8n.notificationlistener.fcc.CaptureAudit
import com.daohoangson.n8n.notificationlistener.fcc.IngestTransform
import com.daohoangson.n8n.notificationlistener.fcc.TransformResult
import com.daohoangson.n8n.notificationlistener.utils.NotificationData
import com.daohoangson.n8n.notificationlistener.utils.NotificationDataExtractor
import com.daohoangson.n8n.notificationlistener.work.IngestScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures notifications and routes financial spends into the fail-safe buffer.
 *
 * The FCC fork replaces upstream's send-then-buffer-on-failure flow: every
 * capture is transformed on-device ([IngestTransform]) and, if it is a financial
 * spend, written to the Room buffer *before* any network call, then a drain is
 * kicked. Non-financial notifications (wrong app, noise, no parseable amount) are
 * dropped here and never leave the device (Branch 5).
 */
@AndroidEntryPoint
class NotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var pendingCaptureDao: PendingCaptureDao

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { statusBarNotification ->
            serviceScope.launch {
                try {
                    val data = NotificationDataExtractor.extractNotificationData(statusBarNotification)
                    processNotification(data)
                } catch (e: Exception) {
                    Log.e(TAG, "capture failed", e)
                }
            }
        }
    }

    private suspend fun processNotification(data: NotificationData) {
        when (val result = IngestTransform.transform(data)) {
            is TransformResult.Dropped -> {
                // Filtered on-device; nothing leaves the phone.
                Log.d(TAG, "dropped ${data.packageName}: ${result.reason}")
                // TEMP (#66 validation): persist drops from TRACKED apps only, so a
                // week of real notifications can be reviewed for a wrongly-dropped
                // purchase. Skip the high-volume untracked-app noise. Remove with CaptureAudit.
                if (!result.reason.startsWith("not a tracked financial app")) {
                    CaptureAudit.record(
                        applicationContext, data.packageName, data.title, data.text,
                        outcome = "dropped", detail = result.reason,
                    )
                }
            }
            is TransformResult.Ingestable -> {
                // Buffer BEFORE any network call: this is the fail-safe (Branch 17).
                val payload = result.payload
                val inserted = pendingCaptureDao.insert(
                    PendingCapture(
                        reference = payload.source.reference,
                        payloadJson = payload.toJson(),
                        account = payload.account,
                    )
                )
                if (inserted == -1L) {
                    Log.d(TAG, "duplicate capture ignored: ${payload.source.reference}")
                    return
                }
                IngestScheduler.drainNow(applicationContext)
                // TEMP (#66 validation): mirror captures into the audit trail. Remove with CaptureAudit.
                CaptureAudit.record(
                    applicationContext, data.packageName, data.title, data.text,
                    outcome = "captured",
                    detail = "${payload.account} ${payload.amount} ${payload.currency}",
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "FccNotificationListener"
    }
}
