package se.apothictech.euthertime.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

object AlarmScheduler {
    const val REMINDER_LEAD_MILLIS = 30 * 60_000L
    const val WAKE_SET_WINDOW_MILLIS = 2 * 60 * 60_000L
    const val AWAKE_CHECK_DELAY_MILLIS = 5 * 60_000L
    const val AWAKE_GUARD_FALLBACK_DELAY_MILLIS = 8 * 60_000L
    private const val MINIMUM_REMINDER_DELAY_MILLIS = 1_000L
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

    fun createWakeAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: Set<Int>,
        nowMillis: Long = System.currentTimeMillis(),
    ): ScheduledAlarm {
        val alarm = ScheduledAlarm(
            id = AlarmStore.nextId(context),
            triggerAtMillis = nextOccurrenceMillis(hour, minute, repeatDays, nowMillis),
            label = label,
            kind = AlarmKind.ALARM,
            repeatDays = repeatDays,
            localHour = hour,
            localMinute = minute,
        )
        schedule(context, alarm)
        return alarm
    }

    fun updateWakeAlarm(
        context: Context,
        id: Int,
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: Set<Int>,
        nowMillis: Long = System.currentTimeMillis(),
    ): ScheduledAlarm? {
        val existing = AlarmStore.get(context, id) ?: return null
        cancelPlatform(context, id)
        val updated = existing.copy(
            triggerAtMillis = nextOccurrenceMillis(hour, minute, repeatDays, nowMillis),
            label = label,
            kind = AlarmKind.ALARM,
            repeatDays = repeatDays,
            localHour = hour,
            localMinute = minute,
        )
        schedule(context, updated)
        return updated
    }

    fun createWakeSet(
        context: Context,
        title: String,
        repeatDays: Set<Int>,
        stages: List<WakeStageDraft>,
        awakeGuardEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ScheduledAlarm> {
        require(stages.size >= 2) { "A wake set needs at least two stages" }
        require(stages.any { it.role == WakeStageRole.FINAL }) { "A wake set needs a final stage" }
        val wakeSetId = AlarmStore.nextId(context)
        return stages.mapIndexed { index, stage ->
                val alarm = ScheduledAlarm(
                    id = AlarmStore.nextId(context),
                    triggerAtMillis = nextOccurrenceMillis(
                        stage.hour,
                        stage.minute,
                        repeatDays,
                        nowMillis,
                    ),
                    label = title,
                    kind = AlarmKind.ALARM,
                    repeatDays = repeatDays,
                    localHour = stage.hour,
                    localMinute = stage.minute,
                    wakeSetId = wakeSetId,
                    stageIndex = index,
                    stageRole = stage.role,
                    awakeGuardEnabled = awakeGuardEnabled,
                )
                schedule(context, alarm)
                alarm
            }
    }

    fun replaceWakeSet(
        context: Context,
        wakeSetId: Int,
        title: String,
        repeatDays: Set<Int>,
        stages: List<WakeStageDraft>,
        awakeGuardEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ScheduledAlarm> {
        require(stages.size >= 2) { "A wake set needs at least two stages" }
        require(stages.any { it.role == WakeStageRole.FINAL }) { "A wake set needs a final stage" }
        AlarmStore.all(context).filter { it.wakeSetId == wakeSetId }.forEach { cancel(context, it.id) }
        return stages.mapIndexed { index, stage ->
                val alarm = ScheduledAlarm(
                    id = AlarmStore.nextId(context),
                    triggerAtMillis = nextOccurrenceMillis(stage.hour, stage.minute, repeatDays, nowMillis),
                    label = title,
                    kind = AlarmKind.ALARM,
                    repeatDays = repeatDays,
                    localHour = stage.hour,
                    localMinute = stage.minute,
                    wakeSetId = wakeSetId,
                    stageIndex = index,
                    stageRole = stage.role,
                    awakeGuardEnabled = awakeGuardEnabled,
                )
                schedule(context, alarm)
                alarm
            }
    }

    fun deleteWakeSet(context: Context, wakeSetId: Int) {
        AlarmStore.all(context).filter { it.wakeSetId == wakeSetId }.forEach { cancel(context, it.id) }
    }

    fun clearWakeSetWithAwakeGuard(context: Context, anchorId: Int) {
        val anchor = AlarmStore.get(context, anchorId) ?: return
        val shouldArmGuard = anchor.wakeSetId != null && anchor.awakeGuardEnabled
        cancelWakeSet(context, anchorId)
        if (!shouldArmGuard) return

        val fallback = ScheduledAlarm(
            id = AlarmStore.nextId(context),
            triggerAtMillis = System.currentTimeMillis() + AWAKE_GUARD_FALLBACK_DELAY_MILLIS,
            label = "${anchor.label} · awake guard",
            kind = AlarmKind.ALARM,
            stageRole = WakeStageRole.FINAL,
            isAwakeGuardFallback = true,
        )
        schedule(context, fallback)
        scheduleAwakeCheck(context, fallback)
    }

    fun confirmAwake(context: Context, fallbackAlarmId: Int) {
        cancel(context, fallbackAlarmId)
        AlarmNotifications.cancelAwakeCheck(context, fallbackAlarmId)
    }

    fun schedule(context: Context, alarm: ScheduledAlarm) {
        require(alarm.triggerAtMillis > System.currentTimeMillis()) { "Alarm must be in the future" }
        AlarmStore.put(context, alarm)
        schedulePlatform(context, alarm)
    }

    fun cancel(context: Context, id: Int) {
        cancelPlatform(context, id)
        AlarmStore.remove(context, id)
    }

    private fun cancelPlatform(context: Context, id: Int) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(triggerIntent(context, id))
        manager.cancel(reminderIntent(context, id))
        manager.cancel(awakeCheckIntent(context, id))
        context.getSystemService(NotificationManager::class.java)
            .cancel(AlarmNotifications.reminderNotificationId(id))
    }

    fun dismissOccurrence(context: Context, id: Int) {
        val alarm = AlarmStore.get(context, id) ?: return
        if (!alarm.repeatsWeekly || alarm.localHour == null || alarm.localMinute == null) {
            cancel(context, id)
            return
        }
        cancelPlatform(context, id)
        schedule(
            context,
            alarm.copy(
                triggerAtMillis = nextOccurrenceMillis(
                    alarm.localHour,
                    alarm.localMinute,
                    alarm.repeatDays,
                    System.currentTimeMillis(),
                ),
            ),
        )
    }

    fun cancelWakeSet(context: Context, anchorId: Int) {
        val anchor = AlarmStore.get(context, anchorId) ?: return
        wakeSet(anchor, AlarmStore.all(context)).forEach { dismissOccurrence(context, it.id) }
    }

    fun hasWakeSetCompanions(context: Context, anchor: ScheduledAlarm): Boolean =
        wakeSet(anchor, AlarmStore.all(context)).any { it.id != anchor.id }

    fun snooze(context: Context, alarm: ScheduledAlarm, minutes: Int = 5) {
        schedule(
            context,
            alarm.copy(triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L),
        )
    }

    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        AlarmStore.all(context).forEach { alarm ->
            if (alarm.repeatsWeekly && alarm.localHour != null && alarm.localMinute != null) {
                val rescheduled = alarm.copy(
                    triggerAtMillis = nextOccurrenceMillis(
                        alarm.localHour,
                        alarm.localMinute,
                        alarm.repeatDays,
                        now,
                    ),
                )
                AlarmStore.put(context, rescheduled)
                schedulePlatform(context, rescheduled)
                if (rescheduled.isAwakeGuardFallback) scheduleAwakeCheck(context, rescheduled)
            } else if (alarm.triggerAtMillis > now) {
                schedulePlatform(context, alarm)
                if (alarm.isAwakeGuardFallback) scheduleAwakeCheck(context, alarm)
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

        reminderTriggerAtMillis(alarm, System.currentTimeMillis())?.let { reminderAtMillis ->
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderAtMillis,
                reminderIntent(context, alarm.id),
            )
        }
    }

    private fun triggerIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun reminderIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AlarmReminderReceiver::class.java).putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun awakeCheckIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AwakeCheckReceiver::class.java).putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    @SuppressLint("MissingPermission")
    private fun scheduleAwakeCheck(context: Context, fallback: ScheduledAlarm) {
        val now = System.currentTimeMillis()
        val checkAtMillis = maxOf(
            fallback.triggerAtMillis - (AWAKE_GUARD_FALLBACK_DELAY_MILLIS - AWAKE_CHECK_DELAY_MILLIS),
            now + MINIMUM_REMINDER_DELAY_MILLIS,
        )
        if (checkAtMillis >= fallback.triggerAtMillis) return
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            checkAtMillis,
            awakeCheckIntent(context, fallback.id),
        )
    }

    internal fun reminderTriggerAtMillis(alarm: ScheduledAlarm, nowMillis: Long): Long? {
        if (alarm.kind != AlarmKind.ALARM || alarm.isAwakeGuardFallback) return null
        val reminderAtMillis = maxOf(
            alarm.triggerAtMillis - REMINDER_LEAD_MILLIS,
            nowMillis + MINIMUM_REMINDER_DELAY_MILLIS,
        )
        return reminderAtMillis.takeIf { it < alarm.triggerAtMillis }
    }

    internal fun wakeSet(anchor: ScheduledAlarm, alarms: List<ScheduledAlarm>): List<ScheduledAlarm> {
        if (anchor.kind != AlarmKind.ALARM) return listOf(anchor)
        anchor.wakeSetId?.let { explicitSetId ->
            return alarms.filter {
                it.wakeSetId == explicitSetId &&
                    it.triggerAtMillis >= anchor.triggerAtMillis &&
                    it.triggerAtMillis <= anchor.triggerAtMillis + 24 * 60 * 60_000L
            }.sortedBy { it.triggerAtMillis }
        }
        val endMillis = anchor.triggerAtMillis + WAKE_SET_WINDOW_MILLIS
        return alarms.filter {
            it.kind == AlarmKind.ALARM &&
                it.triggerAtMillis >= anchor.triggerAtMillis &&
                it.triggerAtMillis <= endMillis
        }.sortedBy { it.triggerAtMillis }
    }

    internal fun nextOccurrenceMillis(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        fromMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        require(hour in 0..23)
        require(minute in 0..59)
        require(repeatDays.all { it in 1..7 })

        val from = Instant.ofEpochMilli(fromMillis).atZone(zoneId)
        for (daysAhead in 0..7) {
            val candidate = from.toLocalDate().plusDays(daysAhead.toLong())
                .atTime(hour, minute)
                .atZone(zoneId)
            if ((repeatDays.isEmpty() || candidate.dayOfWeek.value in repeatDays) &&
                candidate.toInstant().toEpochMilli() > fromMillis
            ) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        error("Could not find next alarm occurrence")
    }
}
