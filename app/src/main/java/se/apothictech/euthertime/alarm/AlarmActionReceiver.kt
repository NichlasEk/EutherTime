package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(context, id)

        when (intent.action) {
            ACTION_CANCEL_UPCOMING -> AlarmScheduler.cancel(context, id)
            ACTION_CANCEL_WAKE_SET -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                AlarmScheduler.cancelWakeSet(context, id)
            }
            ACTION_SNOOZE -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                if (alarm != null) AlarmScheduler.snooze(context, alarm)
            }
            else -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                AlarmScheduler.cancel(context, id)
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "se.apothictech.euthertime.DISMISS"
        const val ACTION_SNOOZE = "se.apothictech.euthertime.SNOOZE"
        const val ACTION_CANCEL_UPCOMING = "se.apothictech.euthertime.CANCEL_UPCOMING"
        const val ACTION_CANCEL_WAKE_SET = "se.apothictech.euthertime.CANCEL_WAKE_SET"
    }
}
