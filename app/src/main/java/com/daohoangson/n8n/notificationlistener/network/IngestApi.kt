package com.daohoangson.n8n.notificationlistener.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * The Financial Command Center private API. Every post carries the shared
 * `X-Ingest-Secret` header (the same secret the backend runs with); the URL is
 * the box's Tailscale address, so the call only succeeds over the tailnet.
 */
interface IngestApi {
    @POST
    suspend fun ingest(
        @Url url: String,
        @Header("X-Ingest-Secret") secret: String,
        @Body body: RequestBody,
    ): Response<Unit>
}
