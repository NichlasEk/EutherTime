package se.apothictech.euthertime.alarm

data class AlarmSignalProfile(
    val gain: Float,
    val vibrationPattern: LongArray,
)

object AlarmSignalProfiles {
    fun forRole(role: WakeStageRole): AlarmSignalProfile = when (role) {
        WakeStageRole.GENTLE -> AlarmSignalProfile(
            gain = 0.28f,
            vibrationPattern = longArrayOf(0, 180, 420, 180, 1_400),
        )
        WakeStageRole.PRIMARY -> AlarmSignalProfile(
            gain = 0.68f,
            vibrationPattern = longArrayOf(0, 420, 260, 420, 900),
        )
        WakeStageRole.FINAL -> AlarmSignalProfile(
            gain = 1.0f,
            vibrationPattern = longArrayOf(0, 650, 180, 650, 350),
        )
    }
}
