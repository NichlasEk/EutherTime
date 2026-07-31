package se.apothictech.euthertime.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_LABEL = "alarm_label"
    const val EXTRA_KIND = "alarm_kind"

    fun create(
        context: Context,
        triggerAtMillis: Long,
        label: String,
        kind: AlarmKind,
    ): ScheduledAlarm {
        val alarm = ScheduledAlarm(
            id = AlarmStore.nextId(context),
            triggerAtMillis = triggerAtMillis,
            label = label,
            kind = kind,
        )
        schedule(context, alarm)
        return alarm
    }

    fun schedule(context: Context, alarm: ScheduledAlarm) {
        require(alarm.triggerAtMillis > System.currentTimeMillis()) { "Alarm must be in the future" }
        AlarmStore.put(context, alarm)
        schedulePlatform(context, alarm)
    }

    fun cancel(context: Context, id: Int) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(triggerIntent(context, id))
        AlarmStore.remove(context, id)
    }

    fun snooze(context: Context, alarm: ScheduledAlarm, minutes: Int = 5) {
        schedule(
            context,
            alarm.copy(triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L),
        )
    }

    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        AlarmStore.all(context).forEach { alarm ->
            if (alarm.triggerAtMillis > now) {
                schedulePlatform(context, alarm)
            } else {
                AlarmStore.remove(context, alarm.id)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun schedulePlatform(context: Context, alarm: ScheduledAlarm) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            throw SecurityException("Exact alarm access is not available")
        }
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            Intent(context, se.apothictech.euthertime.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(
            AlarmManager.AlarmClockInfo(alarm.triggerAtMillis, showIntent),
            triggerIntent(context, alarm.id),
        )
    }

    private fun triggerIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
