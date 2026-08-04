package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MorningJournalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rating = runCatching { WakeRating.valueOf(intent.getStringExtra(EXTRA_RATING).orEmpty()) }.getOrNull() ?: return
        val wakeSetId = intent.getIntExtra(EXTRA_WAKE_SET_ID, -1).takeIf { it >= 0 }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Morning link"
        WakeJournalStore.record(context, wakeSetId, title, rating)
        AlarmNotifications.cancelMorningJournal(context, wakeSetId ?: 0)
    }

    companion object {
        const val EXTRA_RATING = "wake_rating"
        const val EXTRA_WAKE_SET_ID = "wake_set_id"
        const val EXTRA_TITLE = "wake_title"
    }
}
