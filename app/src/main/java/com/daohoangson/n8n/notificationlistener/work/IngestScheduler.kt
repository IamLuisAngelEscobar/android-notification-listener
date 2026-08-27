package com.daohoangson.n8n.notificationlistener.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the drain job. Two triggers:
 *  - [drainNow] right after a capture is buffered, so a delivery attempt follows
 *    promptly when there is connectivity;
 *  - [ensurePeriodicDrain] a ~15-minute safety net so anything left buffered
 *    (app killed mid-retry, long outage) still drains without a new capture.
 *
 * Both require `NetworkType.CONNECTED` and back off exponentially, so retries
 * don't spin while offline.
 */
object IngestScheduler {
    private const val UNIQUE_NOW = "fcc-ingest-drain-now"
    private const val UNIQUE_PERIODIC = "fcc-ingest-drain-periodic"
    private const val UNIQUE_SMS_SWEEP = "fcc-sms-sweep-periodic"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun drainNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<IngestDrainWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun ensurePeriodicDrain(context: Context) {
        val request = PeriodicWorkRequestBuilder<IngestDrainWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Periodic READ_SMS sweep (ADR-0013) that re-reads the inbox for any tracked
     * SMS the live [com.daohoangson.n8n.notificationlistener.SmsReceiver] missed.
     * Deliberately has **no** network constraint — reading SMS into the buffer
     * works offline; the drain worker handles delivery.
     */
    fun ensurePeriodicSmsSweep(context: Context) {
        val request = PeriodicWorkRequestBuilder<SmsSweepWorker>(6, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_SMS_SWEEP, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
