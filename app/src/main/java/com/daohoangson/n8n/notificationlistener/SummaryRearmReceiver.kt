package com.daohoangson.n8n.notificationlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daohoangson.n8n.notificationlistener.work.SummaryAlarm

/**
 * Re-arms the 9 PM summary alarm (#80) whenever the system could have lost or
 * mistimed it:
 * - a reboot cancels every alarm;
 * - a time-zone or clock change moves "21:00 local";
 * - an app update drops the old alarm.
 *
 * All four broadcasts are exempt from Android's implicit-broadcast limits.
 */
class SummaryRearmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in REARM_ACTIONS) {
            SummaryAlarm.arm(context.applicationContext)
        }
    }

    private companion object {
        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
