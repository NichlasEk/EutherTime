package se.apothictech.euthertime.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmReminderSchedulingTest {
    private val now = 1_000_000L

    @Test
    fun alarmGetsReminderExactlyThirtyMinutesBeforeItRings() {
        val alarm = scheduled(AlarmKind.ALARM, now + 3_600_000L)

        assertEquals(
            now + 1_800_000L,
            AlarmScheduler.reminderTriggerAtMillis(alarm, now),
        )
    }

    @Test
    fun timersAndEggProtocolDoNotGetPreAlarmNotifications() {
        assertNull(AlarmScheduler.reminderTriggerAtMillis(scheduled(AlarmKind.TIMER, now + 3_600_000L), now))
        assertNull(AlarmScheduler.reminderTriggerAtMillis(scheduled(AlarmKind.EGG, now + 3_600_000L), now))
    }

    @Test
    fun alarmInsideThirtyMinuteWindowGetsAnAlmostImmediateReminder() {
        assertEquals(
            now + 1_000L,
            AlarmScheduler.reminderTriggerAtMillis(scheduled(AlarmKind.ALARM, now + 900_000L), now),
        )
    }

    @Test
    fun alarmTooCloseToFitReminderDoesNotScheduleAfterItRings() {
        assertNull(AlarmScheduler.reminderTriggerAtMillis(scheduled(AlarmKind.ALARM, now + 500L), now))
    }

    @Test
    fun awakeGuardFallbackDoesNotCreateAnotherPreAlarmReminder() {
        assertNull(
            AlarmScheduler.reminderTriggerAtMillis(
                scheduled(AlarmKind.ALARM, now + 8 * 60_000L).copy(isAwakeGuardFallback = true),
                now,
            ),
        )
    }

    private fun scheduled(kind: AlarmKind, triggerAtMillis: Long) = ScheduledAlarm(
        id = 1000,
        triggerAtMillis = triggerAtMillis,
        label = "MORNING WAKE",
        kind = kind,
    )
}
