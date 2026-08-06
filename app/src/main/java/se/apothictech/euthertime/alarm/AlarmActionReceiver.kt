package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(context, id)

        if (intent.action in ringingActions) ActiveAlarmStore.clear(context, id)

        when (intent.action) {
            ACTION_CANCEL_UPCOMING -> AlarmScheduler.dismissOccurrence(context, id)
            ACTION_CANCEL_WAKE_SET -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                AlarmScheduler.cancelWakeSet(context, id)
            }
            ACTION_CLEAR_WAKE_SET_WITH_GUARD -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                AlarmScheduler.clearWakeSetWithAwakeGuard(context, id)
                if (alarm != null && !alarm.awakeGuardEnabled) {
                    AlarmNotifications.showMorningJournal(context, alarm.wakeSetId, alarm.label)
                }
            }
            ACTION_CONFIRM_AWAKE -> {
                AlarmScheduler.confirmAwake(context, id)
                if (alarm != null) {
                    AlarmNotifications.showMorningJournal(
                        context,
                        alarm.wakeSetId,
                        alarm.label.removeSuffix(" · awake guard"),
                    )
                }
            }
            ACTION_SNOOZE -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                if (alarm != null) AlarmScheduler.snooze(context, alarm)
            }
            else -> {
                context.stopService(Intent(context, AlarmSoundService::class.java))
                AlarmScheduler.dismissOccurrence(context, id)
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "se.apothictech.euthertime.DISMISS"
        const val ACTION_SNOOZE = "se.apothictech.euthertime.SNOOZE"
        const val ACTION_CANCEL_UPCOMING = "se.apothictech.euthertime.CANCEL_UPCOMING"
        const val ACTION_CANCEL_WAKE_SET = "se.apothictech.euthertime.CANCEL_WAKE_SET"
        const val ACTION_CLEAR_WAKE_SET_WITH_GUARD = "se.apothictech.euthertime.CLEAR_WAKE_SET_WITH_GUARD"
        const val ACTION_CONFIRM_AWAKE = "se.apothictech.euthertime.CONFIRM_AWAKE"

        private val ringingActions = setOf(
            ACTION_DISMISS,
            ACTION_SNOOZE,
            ACTION_CLEAR_WAKE_SET_WITH_GUARD,
        )
    }
}
