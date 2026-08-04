package se.apothictech.euthertime.alarm

data class ScheduledAlarm(
    val id: Int,
    val triggerAtMillis: Long,
    val label: String,
    val kind: AlarmKind,
    val repeatDays: Set<Int> = emptySet(),
    val localHour: Int? = null,
    val localMinute: Int? = null,
    val wakeSetId: Int? = null,
    val stageIndex: Int = 0,
    val stageRole: WakeStageRole = WakeStageRole.PRIMARY,
    val awakeGuardEnabled: Boolean = false,
    val isAwakeGuardFallback: Boolean = false,
) {
    val repeatsWeekly: Boolean
        get() = kind == AlarmKind.ALARM && repeatDays.isNotEmpty()
}

enum class WakeStageRole {
    GENTLE,
    PRIMARY,
    FINAL,
}

data class WakeStageDraft(
    val hour: Int,
    val minute: Int,
    val role: WakeStageRole,
)

enum class AlarmKind {
    ALARM,
    TIMER,
    EGG,
}
