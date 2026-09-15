package com.daohoangson.n8n.notificationlistener.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.daohoangson.n8n.notificationlistener.SummaryAlarmReceiver
import com.daohoangson.n8n.notificationlistener.fcc.SummarySchedule
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The 9 PM daily-summary trigger (#80), replacing the Tasker job ADR-0011 §3 named.
 *
 * WorkManager can't promise a wall-clock time, so the *when* is an exact alarm
 * (`setExactAndAllowWhileIdle`, which fires in Doze). The *send* is a WorkManager
 * job, so it waits for connectivity and retries. `USE_EXACT_ALARM` is granted at
 * install for this sideloaded app. The alarm is one-shot: it re-arms itself each
 * time it fires, and [com.daohoangson.n8n.notificationlistener.SummaryRearmReceiver]
 * re-arms it after a reboot, a time-zone or clock change, or an app update.
 */
object SummaryAlarm {
    const val EXTRA_DATE = "fcc.summary.date"
    private const val REQUEST_CODE = 2100
    private const val TAG = "FccSummaryAlarm"

    /** Arms the next 21:00 on the phone's current zone, replacing any pending alarm. */
    fun arm(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val next = SummarySchedule.nextTrigger(now)
        val intent = Intent(context, SummaryAlarmReceiver::class.java)
            .putExtra(EXTRA_DATE, next.toLocalDate().toString())
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = next.toInstant().toEpochMilli()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Not expected (USE_EXACT_ALARM is install-time), but a late summary
            // beats none.
            Log.w(TAG, "exact alarms unavailable; arming an inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        Log.i(TAG, "summary alarm armed for $next")
    }

    /**
     * Hands one day's send to WorkManager. The work is unique per date, so a
     * duplicate alarm delivery can't send the same day twice.
     */
    fun enqueueSend(context: Context, date: LocalDate) {
        val request = OneTimeWorkRequestBuilder<SummaryTriggerWorker>()
            .setInputData(workDataOf(SummaryTriggerWorker.KEY_DATE to date.toString()))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("fcc-summary-$date", ExistingWorkPolicy.KEEP, request)
    }
}
