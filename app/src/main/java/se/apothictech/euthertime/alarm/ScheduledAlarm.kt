package se.apothictech.euthertime.alarm

data class ScheduledAlarm(
    val id: Int,
    val triggerAtMillis: Long,
    val label: String,
    val kind: AlarmKind,
)

enum class AlarmKind {
    ALARM,
    TIMER,
    EGG,
}
