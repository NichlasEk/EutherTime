package se.apothictech.euthertime.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmWakeSetTest {
    private val start = 1_000_000L

    @Test
    fun wakeSetIncludesNearbyWakeAlarmsButNotTimersOrLaterAlarms() {
        val first = alarm(1, start, AlarmKind.ALARM)
        val backup = alarm(2, start + 15 * 60_000L, AlarmKind.ALARM)
        val timer = alarm(3, start + 20 * 60_000L, AlarmKind.TIMER)
        val later = alarm(4, start + AlarmScheduler.WAKE_SET_WINDOW_MILLIS + 1L, AlarmKind.ALARM)

        assertEquals(listOf(first, backup), AlarmScheduler.wakeSet(first, listOf(later, timer, backup, first)))
    }

    @Test
    fun dismissingBackupDoesNotReachBackAndCancelEarlierAlarm() {
        val first = alarm(1, start, AlarmKind.ALARM)
        val backup = alarm(2, start + 15 * 60_000L, AlarmKind.ALARM)

        assertEquals(listOf(backup), AlarmScheduler.wakeSet(backup, listOf(first, backup)))
    }

    private fun alarm(id: Int, triggerAtMillis: Long, kind: AlarmKind) = ScheduledAlarm(
        id = id,
        triggerAtMillis = triggerAtMillis,
        label = "Wake $id",
        kind = kind,
    )
}
