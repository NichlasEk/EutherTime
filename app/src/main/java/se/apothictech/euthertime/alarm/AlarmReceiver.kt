package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(context, id) ?: return
        AlarmNotifications.cancelReminder(context, alarm.id)
        if (alarm.isAwakeGuardFallback) AlarmNotifications.cancelAwakeCheck(context, alarm.id)
        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmSoundService::class.java)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                .putExtra(AlarmScheduler.EXTRA_LABEL, alarm.label)
                .putExtra(AlarmScheduler.EXTRA_KIND, alarm.kind.name),
        )
    }
}
