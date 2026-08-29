package com.daohoangson.n8n.notificationlistener.work

import android.content.Context
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daohoangson.n8n.notificationlistener.data.database.PendingCapture
import com.daohoangson.n8n.notificationlistener.data.database.PendingCaptureDao
import com.daohoangson.n8n.notificationlistener.fcc.IngestTransform
import com.daohoangson.n8n.notificationlistener.fcc.TransformResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * READ_SMS backstop for [com.daohoangson.n8n.notificationlistener.SmsReceiver]
 * (ADR-0013): periodically re-reads recent inbox SMS and buffers any
 * tracked-sender spend the live broadcast missed (app killed at receive time, a
 * dropped broadcast).
 *
 * Re-reading a message the broadcast already captured is a no-op:
 * [IngestTransform.transformSms] derives the reference from (sender, body), not
 * the timestamp, so the sweep and the broadcast agree on the same `sms-…`
 * reference and the buffer's UNIQUE index ignores the duplicate.
 *
 * If READ_SMS isn't granted the content query throws `SecurityException`; we
 * swallow it and succeed (nothing to do) rather than spin on retry. Unlike the
 * drain, this needs no network — reading SMS into the buffer works offline; the
 * drain worker handles delivery separately.
 */
@HiltWorker
class SmsSweepWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingCaptureDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sinceMillis = System.currentTimeMillis() - LOOKBACK_MS
        var buffered = 0
        try {
            applicationContext.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sinceMillis.toString()),
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addrIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val date = cursor.getLong(dateIdx)
                    val result = IngestTransform.transformSms(sender, body, date)
                    if (result is TransformResult.Ingestable) {
                        val inserted = dao.insert(
                            PendingCapture(
                                reference = result.payload.source.reference,
                                payloadJson = result.payload.toJson(),
                                account = result.payload.account,
                            )
                        )
                        if (inserted != -1L) buffered++
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_SMS not granted; sweep is a no-op until it is")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "sms sweep failed", e)
            return Result.success()
        }

        if (buffered > 0) IngestScheduler.drainNow(applicationContext)
        return Result.success()
    }

    companion object {
        private const val TAG = "SmsSweepWorker"
        private const val LOOKBACK_MS = 24L * 60 * 60 * 1000 // re-scan the last day
    }
}
