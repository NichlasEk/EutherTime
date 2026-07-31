package se.apothictech.euthertime.alarm

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object AlarmStore {
    private const val PREFS_NAME = "euthertime_schedule"
    private const val KEY_ALARMS = "alarms"
    private const val KEY_NEXT_ID = "next_id"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun nextId(context: Context): Int {
        val preferences = prefs(context)
        val next = preferences.getInt(KEY_NEXT_ID, 1000)
        preferences.edit { putInt(KEY_NEXT_ID, if (next == Int.MAX_VALUE) 1000 else next + 1) }
        return next
    }

    @Synchronized
    fun all(context: Context): List<ScheduledAlarm> {
        val raw = prefs(context).getString(KEY_ALARMS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ScheduledAlarm(
                            id = item.getInt("id"),
                            triggerAtMillis = item.getLong("triggerAtMillis"),
                            label = item.getString("label"),
                            kind = AlarmKind.valueOf(item.getString("kind")),
                        ),
                    )
                }
            }.sortedBy { it.triggerAtMillis }
        }.getOrDefault(emptyList())
    }

    fun get(context: Context, id: Int): ScheduledAlarm? = all(context).firstOrNull { it.id == id }

    @Synchronized
    fun put(context: Context, alarm: ScheduledAlarm) {
        val updated = all(context).filterNot { it.id == alarm.id } + alarm
        write(context, updated)
    }

    @Synchronized
    fun remove(context: Context, id: Int) {
        write(context, all(context).filterNot { it.id == id })
    }

    @Synchronized
    fun removeExpired(context: Context, now: Long = System.currentTimeMillis()) {
        write(context, all(context).filter { it.triggerAtMillis >= now })
    }

    private fun write(context: Context, alarms: List<ScheduledAlarm>) {
        val array = JSONArray()
        alarms.sortedBy { it.triggerAtMillis }.forEach { alarm ->
            array.put(
                JSONObject()
                    .put("id", alarm.id)
                    .put("triggerAtMillis", alarm.triggerAtMillis)
                    .put("label", alarm.label)
                    .put("kind", alarm.kind.name),
            )
        }
        prefs(context).edit { putString(KEY_ALARMS, array.toString()) }
    }
}
