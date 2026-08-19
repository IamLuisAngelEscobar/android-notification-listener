package com.daohoangson.n8n.notificationlistener.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daohoangson.n8n.notificationlistener.data.database.PendingCaptureDao
import com.daohoangson.n8n.notificationlistener.fcc.IngestEndpoint
import com.daohoangson.n8n.notificationlistener.network.IngestApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Drains the [PendingCapture][com.daohoangson.n8n.notificationlistener.data.database.PendingCapture]
 * buffer to `/ingest`, replacing upstream's manual, UI-driven retry with an
 * automatic background job. Scheduled with `NetworkType.CONNECTED` + exponential
 * backoff (see [IngestScheduler]), so a cellular deadzone or a restarting home
 * server never drops a capture — the row waits in Room until a post succeeds.
 *
 * Delivery policy per row:
 *  - **2xx** (a new 201 *or* a 200 `duplicate: true`): delivered → delete.
 *  - **401 / 5xx / IO / timeout**: transient (bad-secret config or server down) →
 *    keep the row, count the attempt, ask WorkManager to retry the whole batch.
 *  - **other 4xx** (e.g. 422 malformed, 400): permanent — a replay can't fix a
 *    bad body, so drop it (logged) rather than wedge the queue behind it.
 */
@HiltWorker
class IngestDrainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingCaptureDao,
    private val api: IngestApi,
    private val endpoint: IngestEndpoint,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (endpoint.url.isBlank() || endpoint.secret.isBlank()) {
            Log.e(TAG, "ingest endpoint/secret not configured (local.properties) — cannot drain")
            return Result.failure()
        }

        val pending = dao.getAll()
        if (pending.isEmpty()) return Result.success()

        var sawTransient = false
        val jsonMedia = "application/json".toMediaType()

        for (row in pending) {
            try {
                val body = row.payloadJson.toRequestBody(jsonMedia)
                val response = api.ingest(endpoint.url, endpoint.secret, body)
                when {
                    response.isSuccessful -> {
                        // 201 new row or 200 duplicate: either way it's landed.
                        dao.deleteById(row.id)
                    }
                    response.code() == 401 || response.code() >= 500 -> {
                        // Bad secret (fixable config) or server down: retry later.
                        dao.incrementAttempt(row.id)
                        sawTransient = true
                    }
                    else -> {
                        // 422/400 and friends: the body itself is rejected; a
                        // replay won't help, so don't block the queue on it.
                        Log.w(TAG, "dropping capture ${row.reference}: HTTP ${response.code()}")
                        dao.deleteById(row.id)
                    }
                }
            } catch (e: IOException) {
                // No connectivity / read timeout: quintessentially transient.
                dao.incrementAttempt(row.id)
                sawTransient = true
            }
        }

        return if (sawTransient) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "IngestDrainWorker"
    }
}
