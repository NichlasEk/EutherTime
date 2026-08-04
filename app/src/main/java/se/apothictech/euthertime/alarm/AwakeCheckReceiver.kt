package se.apothictech.euthertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AwakeCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fallbackId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val fallback = AlarmStore.get(context, fallbackId) ?: return
        if (fallback.isAwakeGuardFallback && fallback.triggerAtMillis > System.currentTimeMillis()) {
            AlarmNotifications.showAwakeCheck(context, fallback)
        }
    }
}
