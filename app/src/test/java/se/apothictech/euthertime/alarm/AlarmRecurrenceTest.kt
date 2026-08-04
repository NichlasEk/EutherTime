package se.apothictech.euthertime.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmRecurrenceTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun oneShotAlarmUsesTodayWhenTimeIsStillAhead() {
        val from = millis(2026, 8, 4, 6, 30)

        assertEquals(
            millis(2026, 8, 4, 7, 0),
            AlarmScheduler.nextOccurrenceMillis(7, 0, emptySet(), from, utc),
        )
    }

    @Test
    fun oneShotAlarmMovesToTomorrowAfterTimeHasPassed() {
        val from = millis(2026, 8, 4, 7, 1)

        assertEquals(
            millis(2026, 8, 5, 7, 0),
            AlarmScheduler.nextOccurrenceMillis(7, 0, emptySet(), from, utc),
        )
    }

    @Test
    fun weeklyAlarmChoosesNextSelectedWeekday() {
        val tuesday = millis(2026, 8, 4, 8, 0)

        assertEquals(
            millis(2026, 8, 5, 7, 0),
            AlarmScheduler.nextOccurrenceMillis(7, 0, setOf(1, 3, 5), tuesday, utc),
        )
    }

    @Test
    fun weeklyAlarmMovesAFullWeekAfterTodaysOccurrence() {
        val mondayAfterAlarm = millis(2026, 8, 3, 7, 1)

        assertEquals(
            millis(2026, 8, 10, 7, 0),
            AlarmScheduler.nextOccurrenceMillis(7, 0, setOf(1), mondayAfterAlarm, utc),
        )
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(utc).toInstant().toEpochMilli()
}
