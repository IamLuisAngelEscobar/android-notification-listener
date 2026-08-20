package com.daohoangson.n8n.notificationlistener.network

import com.daohoangson.n8n.notificationlistener.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    // Retrofit requires a base URL even though every call passes an absolute
    // @Url (the ingest endpoint comes from BuildConfig at call time). This
    // placeholder only satisfies that requirement; the absolute @Url overrides
    // it per request. Without it, Retrofit.build() throws "Base URL required."
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val webhookApi: WebhookApi = retrofit.create(WebhookApi::class.java)

    val ingestApi: IngestApi = retrofit.create(IngestApi::class.java)
}