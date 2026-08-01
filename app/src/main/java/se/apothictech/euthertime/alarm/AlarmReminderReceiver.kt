package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(context, id) ?: return
        if (alarm.kind != AlarmKind.ALARM || alarm.triggerAtMillis <= System.currentTimeMillis()) return

        AlarmNotifications.showReminder(context, alarm)
    }
}
