package se.apothictech.euthertime.alarm

import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSignalProfileTest {
    @Test
    fun wakeStagesIncreaseSignalGain() {
        val gentle = AlarmSignalProfiles.forRole(WakeStageRole.GENTLE)
        val primary = AlarmSignalProfiles.forRole(WakeStageRole.PRIMARY)
        val final = AlarmSignalProfiles.forRole(WakeStageRole.FINAL)

        assertTrue(gentle.gain < primary.gain)
        assertTrue(primary.gain < final.gain)
    }

    @Test
    fun everyWakeStageHasARepeatingVibrationPattern() {
        WakeStageRole.entries.forEach { role ->
            val pattern = AlarmSignalProfiles.forRole(role).vibrationPattern
            assertTrue(pattern.size >= 3)
            assertTrue(pattern.drop(1).all { it > 0L })
        }
    }
}
