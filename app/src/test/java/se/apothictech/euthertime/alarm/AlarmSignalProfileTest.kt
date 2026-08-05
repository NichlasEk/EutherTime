package se.apothictech.euthertime.alarm

import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSignalProfileTest {
    @Test
    fun wakeStagesIncreaseSignalGain() {
        val gentle = AlarmSignalProfiles.forRole(WakeStageRole.GENTLE)
        val primary = AlarmSignalProfiles.forRole(WakeStageRole.PRIMARY)
        val final = AlarmSignalProfiles.forRole(WakeStageRole.FINAL)

        assertTrue(gentle.startGain < primary.startGain)
        assertTrue(primary.startGain < final.startGain)
        assertTrue(gentle.targetGain < primary.targetGain)
        assertTrue(primary.targetGain < final.targetGain)
    }

    @Test
    fun everyWakeStageHasARepeatingVibrationPattern() {
        WakeStageRole.entries.forEach { role ->
            val pattern = AlarmSignalProfiles.forRole(role).vibrationPattern
            assertTrue(pattern.size >= 3)
            assertTrue(pattern.drop(1).all { it > 0L })
        }
    }

    @Test
    fun everyWakeStageRampsSmoothlyToItsTargetGain() {
        WakeStageRole.entries.forEach { role ->
            val profile = AlarmSignalProfiles.forRole(role)

            assertTrue(profile.gainAt(0L) == profile.startGain)
            assertTrue(profile.gainAt(profile.rampDurationMillis / 2) in profile.startGain..profile.targetGain)
            assertTrue(profile.gainAt(profile.rampDurationMillis) == profile.targetGain)
            assertTrue(profile.gainAt(profile.rampDurationMillis + 60_000L) == profile.targetGain)
        }
    }
}
