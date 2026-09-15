package com.daohoangson.n8n.notificationlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daohoangson.n8n.notificationlistener.work.SummaryAlarm
import java.time.LocalDate

/**
 * Fires at 9 PM phone-local (#80). It queues that day's summary send, then arms
 * tomorrow's alarm.
 *
 * The date comes from the alarm itself rather than "now", so a delivery that
 * lands a little late still asks for the day it was scheduled for.
 */
class SummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val date = intent.getStringExtra(SummaryAlarm.EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        SummaryAlarm.enqueueSend(appContext, date)
        SummaryAlarm.arm(appContext)
    }
}
