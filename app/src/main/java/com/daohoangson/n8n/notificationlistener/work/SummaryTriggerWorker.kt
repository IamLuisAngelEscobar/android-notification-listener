package com.daohoangson.n8n.notificationlistener.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daohoangson.n8n.notificationlistener.fcc.IngestEndpoint
import com.daohoangson.n8n.notificationlistener.fcc.SummarySchedule
import com.daohoangson.n8n.notificationlistener.network.IngestApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.time.LocalDate

/**
 * Asks the backend to push one day's CSP summary (#80), for the local date the 9 PM
 * alarm fired on (see [SummaryAlarm]).
 *
 * Scheduled with `NetworkType.CONNECTED` and exponential backoff, so a phone that's
 * off the tailnet at 9 PM still gets the summary once it reconnects. Retries stop
 * after [MAX_ATTEMPTS] (about two hours of backoff), so a summary never lands days
 * late; for a missed night, message the bot `summary` instead.
 */
@HiltWorker
class SummaryTriggerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: IngestApi,
    private val endpoint: IngestEndpoint,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val date = inputData.getString(KEY_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (date == null) {
            Log.e(TAG, "no valid date in the work input; dropping")
            return Result.failure()
        }
        if (endpoint.url.isBlank() || endpoint.secret.isBlank()) {
            Log.e(TAG, "ingest endpoint/secret not configured (local.properties) — cannot send summary")
            return Result.failure()
        }
        if (runAttemptCount >= MAX_ATTEMPTS) {
            Log.w(TAG, "giving up on the $date summary after $runAttemptCount attempts")
            return Result.failure()
        }

        val url = try {
            SummarySchedule.summaryUrl(endpoint.url, date)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, e.message ?: "bad ingest URL")
            return Result.failure()
        }

        return try {
            val response = api.sendSummary(url, endpoint.secret)
            when (SummarySchedule.outcomeFor(response.code())) {
                SummarySchedule.Outcome.SENT -> {
                    Log.i(TAG, "summary sent for $date")
                    Result.success()
                }
                SummarySchedule.Outcome.RETRY -> {
                    Log.w(TAG, "summary for $date: HTTP ${response.code()}, will retry")
                    Result.retry()
                }
                SummarySchedule.Outcome.GIVE_UP -> {
                    Log.w(TAG, "summary for $date: HTTP ${response.code()}, giving up")
                    Result.failure()
                }
            }
        } catch (e: IOException) {
            // No connectivity or a read timeout: transient.
            Result.retry()
        }
    }

    companion object {
        const val KEY_DATE = "date"
        private const val MAX_ATTEMPTS = 8
        private const val TAG = "FccSummaryTrigger"
    }
}
