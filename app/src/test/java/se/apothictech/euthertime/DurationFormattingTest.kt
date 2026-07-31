package se.apothictech.euthertime

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormattingTest {
    @Test
    fun eggPresetDurationsStayHumanReadable() {
        assertEquals("05:30", duration(330_000L))
        assertEquals("01:05:03", duration(3_903_000L))
    }

    @Test
    fun exactCountdownIncludesHoursMinutesAndSeconds() {
        assertEquals(3_723_000L, durationMillis(hours = 1, minutes = 2, seconds = 3))
        assertEquals(59_000L, durationMillis(hours = 0, minutes = 0, seconds = 59))
    }

    private fun duration(millis: Long): String {
        val totalSeconds = millis / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }
}
