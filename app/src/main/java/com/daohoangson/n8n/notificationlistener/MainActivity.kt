package com.daohoangson.n8n.notificationlistener

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.daohoangson.n8n.notificationlistener.data.repository.ListenedAppRepository
import com.daohoangson.n8n.notificationlistener.data.repository.NotificationRepository
import com.daohoangson.n8n.notificationlistener.ui.NotificationListActivity
import com.daohoangson.n8n.notificationlistener.ui.apps.AppsScreen
import com.daohoangson.n8n.notificationlistener.ui.onboarding.OnboardingScreen
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: NotificationRepository

    @Inject
    lateinit var listenedApps: ListenedAppRepository

    // Re-read on every resume: the grant is flipped in Android settings, and the
    // app must notice it when the owner comes back.
    private var isPermissionGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ListenerTheme {
                val failedCount by repository.getFailedNotificationCountFlow().collectAsState(initial = 0)
                val undecidedCount by repository.getUndecidedNotificationCountFlow().collectAsState(initial = 0)
                if (isPermissionGranted) {
                    AppsScreen(
                        listenedApps = listenedApps,
                        reviewCount = failedCount + undecidedCount,
                        onReviewCaptures = { openNotificationListActivity() },
                    )
                } else {
                    OnboardingScreen(onGrant = { openNotificationListenerSettings() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isPermissionGranted = isNotificationListenerEnabled()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun openNotificationListActivity() {
        val intent = Intent(this, NotificationListActivity::class.java)
        startActivity(intent)
    }

}
