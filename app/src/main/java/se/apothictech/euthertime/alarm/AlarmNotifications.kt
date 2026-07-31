package se.apothictech.euthertime.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import se.apothictech.euthertime.R

object AlarmNotifications {
    const val CHANNEL_ID = "euthertime_alarm_v1"

    fun notificationId(alarmId: Int): Int = 20_000 + (alarmId % 10_000)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            enableLights(true)
            lightColor = Color.rgb(116, 255, 99)
            enableVibration(false)
            setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, alarm: ScheduledAlarm): Notification {
        ensureChannel(context)
        val ringIntent = Intent(context, AlarmRingActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            .putExtra(AlarmScheduler.EXTRA_LABEL, alarm.label)
            .putExtra(AlarmScheduler.EXTRA_KIND, alarm.kind.name)
        val ringPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = actionIntent(context, alarm.id, AlarmActionReceiver.ACTION_DISMISS)
        val snoozeIntent = actionIntent(context, alarm.id, AlarmActionReceiver.ACTION_SNOOZE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_euthertime)
            .setColor(Color.rgb(116, 255, 99))
            .setContentTitle(alarm.label)
            .setContentText(messageFor(alarm.kind))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(ringPendingIntent)
            .setFullScreenIntent(ringPendingIntent, true)
            .addAction(0, context.getString(R.string.snooze), snoozeIntent)
            .addAction(0, context.getString(R.string.dismiss), dismissIntent)
            .build()
    }

    private fun actionIntent(context: Context, alarmId: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId + action.hashCode(),
            Intent(context, AlarmActionReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun messageFor(kind: AlarmKind): String = when (kind) {
        AlarmKind.ALARM -> "WAKE PROTOCOL ACTIVE"
        AlarmKind.TIMER -> "COUNTDOWN COMPLETE"
        AlarmKind.EGG -> "EXTRACTION REQUIRED"
    }
}
