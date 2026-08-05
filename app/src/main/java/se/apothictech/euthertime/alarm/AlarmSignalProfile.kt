package se.apothictech.euthertime.alarm

import androidx.annotation.RawRes
import se.apothictech.euthertime.R

data class AlarmSignalProfile(
    val startGain: Float,
    val targetGain: Float,
    val rampDurationMillis: Long,
    val vibrationPattern: LongArray,
) {
    fun gainAt(elapsedMillis: Long): Float {
        if (rampDurationMillis <= 0L) return targetGain
        val progress = (elapsedMillis.coerceIn(0L, rampDurationMillis).toFloat() / rampDurationMillis)
        return startGain + (targetGain - startGain) * progress
    }
}

enum class AlarmSoundProfile(val displayName: String, val description: String) {
    SYSTEM("SYSTEM", "Phone alarm tone"),
    NEON_DAWN("NEON DAWN", "Warm bells · 68 BPM"),
    PULSE_GRID("PULSE GRID", "Bright pulse · 92 BPM"),
    RED_SHIFT("RED SHIFT", "Urgent drive · 124 BPM"),
}

object AlarmSoundAssets {
    @RawRes
    fun rawResourceFor(soundProfile: AlarmSoundProfile): Int? = when (soundProfile) {
        AlarmSoundProfile.SYSTEM -> null
        AlarmSoundProfile.NEON_DAWN -> R.raw.euthertime_neon_dawn
        AlarmSoundProfile.PULSE_GRID -> R.raw.euthertime_pulse_grid
        AlarmSoundProfile.RED_SHIFT -> R.raw.euthertime_red_shift
    }
}

object AlarmSignalProfiles {
    fun forRole(role: WakeStageRole): AlarmSignalProfile = when (role) {
        WakeStageRole.GENTLE -> AlarmSignalProfile(
            startGain = 0.08f,
            targetGain = 0.32f,
            rampDurationMillis = 60_000L,
            vibrationPattern = longArrayOf(0, 180, 420, 180, 1_400),
        )
        WakeStageRole.PRIMARY -> AlarmSignalProfile(
            startGain = 0.22f,
            targetGain = 0.72f,
            rampDurationMillis = 45_000L,
            vibrationPattern = longArrayOf(0, 420, 260, 420, 900),
        )
        WakeStageRole.FINAL -> AlarmSignalProfile(
            startGain = 0.50f,
            targetGain = 1.0f,
            rampDurationMillis = 25_000L,
            vibrationPattern = longArrayOf(0, 650, 180, 650, 350),
        )
    }
}
