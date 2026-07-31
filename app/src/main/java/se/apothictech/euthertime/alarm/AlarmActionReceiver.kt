package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(context, id)
        context.stopService(Intent(context, AlarmSoundService::class.java))

        if (intent.action == ACTION_SNOOZE && alarm != null) {
            AlarmScheduler.snooze(context, alarm)
        } else {
            AlarmScheduler.cancel(context, id)
        }
    }

    companion object {
        const val ACTION_DISMISS = "se.apothictech.euthertime.DISMISS"
        const val ACTION_SNOOZE = "se.apothictech.euthertime.SNOOZE"
    }
}
