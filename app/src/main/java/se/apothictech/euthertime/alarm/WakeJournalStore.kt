package se.apothictech.euthertime.alarm

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class WakeRating { DEAD, OKAY, SHARP }

data class WakeJournalEntry(val recordedAtMillis: Long, val wakeSetId: Int?, val title: String, val rating: WakeRating)

object WakeJournalStore {
    private const val PREFS = "euthertime_wake_journal"
    private const val KEY = "entries"

    private fun prefs(context: Context) = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun entries(context: Context): List<WakeJournalEntry> = runCatching {
        val array = JSONArray(prefs(context).getString(KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    WakeJournalEntry(
                        recordedAtMillis = item.getLong("recordedAtMillis"),
                        wakeSetId = item.optInt("wakeSetId").takeIf { item.has("wakeSetId") },
                        title = item.getString("title"),
                        rating = WakeRating.valueOf(item.getString("rating")),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun record(context: Context, wakeSetId: Int?, title: String, rating: WakeRating) {
        val updated = (entries(context) + WakeJournalEntry(System.currentTimeMillis(), wakeSetId, title, rating)).takeLast(365)
        val array = JSONArray()
        updated.forEach { entry ->
            array.put(
                JSONObject()
                    .put("recordedAtMillis", entry.recordedAtMillis)
                    .put("title", entry.title)
                    .put("rating", entry.rating.name)
                    .apply { entry.wakeSetId?.let { put("wakeSetId", it) } },
            )
        }
        prefs(context).edit { putString(KEY, array.toString()) }
    }
}
