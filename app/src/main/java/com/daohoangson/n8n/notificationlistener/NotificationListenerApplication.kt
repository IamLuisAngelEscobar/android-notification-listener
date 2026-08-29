package com.daohoangson.n8n.notificationlistener

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.daohoangson.n8n.notificationlistener.work.IngestScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NotificationListenerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // On-demand WorkManager init (the default initializer is removed in the
    // manifest) so the Hilt-aware worker factory is wired in.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Safety net: drain anything left buffered even without a fresh capture.
        IngestScheduler.ensurePeriodicDrain(this)
        // ADR-0013: re-read the SMS inbox periodically for anything the live
        // broadcast missed (needs READ_SMS; a no-op until that's granted).
        IngestScheduler.ensurePeriodicSmsSweep(this)
    }
}
