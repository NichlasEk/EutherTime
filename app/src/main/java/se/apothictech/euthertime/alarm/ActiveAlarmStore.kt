package se.apothictech.euthertime.alarm

import android.content.Context
import androidx.core.content.edit

internal object ActiveAlarmSessionPolicy {
    const val MAX_ACTIVE_MILLIS = 4 * 60 * 60_000L

    fun isFresh(startedAtMillis: Long, nowMillis: Long): Boolean =
        startedAtMillis > 0L &&
            nowMillis >= startedAtMillis &&
            nowMillis - startedAtMillis <= MAX_ACTIVE_MILLIS
}

object ActiveAlarmStore {
    private const val PREFS_NAME = "euthertime_active_alarm"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_STARTED_AT = "started_at"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun markActive(context: Context, alarmId: Int, nowMillis: Long = System.currentTimeMillis()) {
        prefs(context).edit {
            putInt(KEY_ALARM_ID, alarmId)
            putLong(KEY_STARTED_AT, nowMillis)
        }
    }

    fun current(context: Context, nowMillis: Long = System.currentTimeMillis()): ScheduledAlarm? {
        val preferences = prefs(context)
        val alarmId = preferences.getInt(KEY_ALARM_ID, -1)
        val startedAt = preferences.getLong(KEY_STARTED_AT, 0L)
        val alarm = alarmId.takeIf { it >= 0 }?.let { AlarmStore.get(context, it) }
        if (alarm == null || !ActiveAlarmSessionPolicy.isFresh(startedAt, nowMillis)) {
            clear(context)
            return null
        }
        return alarm
    }

    fun clear(context: Context, alarmId: Int? = null) {
        val preferences = prefs(context)
        if (alarmId != null && preferences.getInt(KEY_ALARM_ID, -1) != alarmId) return
        preferences.edit { clear() }
    }
}
