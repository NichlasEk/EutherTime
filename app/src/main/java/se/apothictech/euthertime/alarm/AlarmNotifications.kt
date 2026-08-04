package se.apothictech.euthertime.alarm

import android.annotation.SuppressLint
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
import java.text.DateFormat
import java.util.Date

object AlarmNotifications {
    const val CHANNEL_ID = "euthertime_alarm_v1"
    const val REMINDER_CHANNEL_ID = "euthertime_pre_alarm_v1"

    fun notificationId(alarmId: Int): Int = 20_000 + (alarmId % 10_000)
    fun reminderNotificationId(alarmId: Int): Int = 40_000 + (alarmId % 10_000)
    fun awakeCheckNotificationId(alarmId: Int): Int = 50_000 + (alarmId % 10_000)
    fun morningJournalNotificationId(wakeSetId: Int): Int = 60_000 + (wakeSetId % 10_000)

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

    fun ensureReminderChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.pre_alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.pre_alarm_channel_description)
            enableLights(true)
            lightColor = Color.rgb(116, 255, 99)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, alarm: ScheduledAlarm) {
        ensureReminderChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            Intent(context, se.apothictech.euthertime.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = actionIntent(
            context,
            alarm.id,
            AlarmActionReceiver.ACTION_CANCEL_UPCOMING,
        )
        val cancelSetIntent = actionIntent(
            context,
            alarm.id,
            AlarmActionReceiver.ACTION_CANCEL_WAKE_SET,
        )
        val alarmTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(alarm.triggerAtMillis))
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_euthertime)
            .setColor(Color.rgb(116, 255, 99))
            .setContentTitle(context.getString(R.string.pre_alarm_title))
            .setContentText(context.getString(R.string.pre_alarm_message, alarm.label, alarmTime))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, context.getString(R.string.disarm_this), cancelIntent)
            .apply {
                if (AlarmScheduler.hasWakeSetCompanions(context, alarm)) {
                    addAction(0, context.getString(R.string.disarm_set), cancelSetIntent)
                }
            }
            .build()

        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(reminderNotificationId(alarm.id), notification)
        }
    }

    fun cancelReminder(context: Context, alarmId: Int) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(reminderNotificationId(alarmId))
    }

    @SuppressLint("MissingPermission")
    fun showAwakeCheck(context: Context, fallback: ScheduledAlarm) {
        ensureReminderChannel(context)
        val confirmIntent = actionIntent(context, fallback.id, AlarmActionReceiver.ACTION_CONFIRM_AWAKE)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_euthertime)
            .setColor(Color.rgb(255, 176, 0))
            .setContentTitle(context.getString(R.string.awake_check_title))
            .setContentText(context.getString(R.string.awake_check_message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, context.getString(R.string.confirm_awake), confirmIntent)
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(awakeCheckNotificationId(fallback.id), notification)
        }
    }

    fun cancelAwakeCheck(context: Context, fallbackAlarmId: Int) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(awakeCheckNotificationId(fallbackAlarmId))
    }

    @SuppressLint("MissingPermission")
    fun showMorningJournal(context: Context, wakeSetId: Int?, title: String) {
        ensureReminderChannel(context)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_euthertime)
            .setColor(Color.rgb(116, 255, 99))
            .setContentTitle(context.getString(R.string.wake_journal_title))
            .setContentText(context.getString(R.string.wake_journal_message, title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(true)
            .setAutoCancel(true)
            .apply {
                WakeRating.entries.forEach { rating ->
                    addAction(0, rating.name, journalIntent(context, wakeSetId, title, rating))
                }
            }
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(morningJournalNotificationId(wakeSetId ?: 0), notification)
    }

    fun cancelMorningJournal(context: Context, wakeSetId: Int) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(morningJournalNotificationId(wakeSetId))
    }

    private fun journalIntent(context: Context, wakeSetId: Int?, title: String, rating: WakeRating): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (wakeSetId ?: 0) + rating.name.hashCode(),
            Intent(context, MorningJournalReceiver::class.java)
                .putExtra(MorningJournalReceiver.EXTRA_WAKE_SET_ID, wakeSetId ?: -1)
                .putExtra(MorningJournalReceiver.EXTRA_TITLE, title)
                .putExtra(MorningJournalReceiver.EXTRA_RATING, rating.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
        val dismissSetIntent = actionIntent(context, alarm.id, AlarmActionReceiver.ACTION_CLEAR_WAKE_SET_WITH_GUARD)
        val hasWakeSetCompanions = AlarmScheduler.hasWakeSetCompanions(context, alarm)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_euthertime)
            .setColor(Color.rgb(116, 255, 99))
            .setContentTitle(
                if (alarm.wakeSetId != null) "${alarm.label} · ${alarm.stageRole.name}" else alarm.label,
            )
            .setContentText(messageFor(alarm.kind))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(ringPendingIntent)
            .setFullScreenIntent(ringPendingIntent, true)
            .addAction(0, context.getString(R.string.snooze), snoozeIntent)
            .addAction(
                0,
                context.getString(if (hasWakeSetCompanions) R.string.next_signal else R.string.dismiss),
                dismissIntent,
            )
            .apply {
                if (hasWakeSetCompanions) {
                    addAction(0, context.getString(R.string.dismiss_set), dismissSetIntent)
                }
            }
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
