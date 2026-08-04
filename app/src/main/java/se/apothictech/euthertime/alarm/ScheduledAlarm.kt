package se.apothictech.euthertime.alarm

data class ScheduledAlarm(
    val id: Int,
    val triggerAtMillis: Long,
    val label: String,
    val kind: AlarmKind,
    val repeatDays: Set<Int> = emptySet(),
    val localHour: Int? = null,
    val localMinute: Int? = null,
) {
    val repeatsWeekly: Boolean
        get() = kind == AlarmKind.ALARM && repeatDays.isNotEmpty()
}

enum class AlarmKind {
    ALARM,
    TIMER,
    EGG,
}
