package se.apothictech.euthertime.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class AlarmIntegrityCheck(
    val label: String,
    val detail: String,
    val nominal: Boolean,
)

object AlarmIntegrityInspector {
    fun inspect(context: Context): List<AlarmIntegrityCheck> {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        AlarmNotifications.ensureChannel(context)
        val exactAlarmReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val notificationPermission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = notificationPermission && NotificationManagerCompat.from(context).areNotificationsEnabled()
        val fullScreenReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            notificationManager.canUseFullScreenIntent()
        val alarmChannelReady = Build.VERSION.SDK_INT < 26 || run {
            val channel = notificationManager.getNotificationChannel(AlarmNotifications.CHANNEL_ID)
            channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        val alarmToneReady = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) != null ||
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) != null
        val nextStored = AlarmStore.all(context).minByOrNull { it.triggerAtMillis }
        val nextPlatform = alarmManager.nextAlarmClock
        val platformCoversStored = nextStored == null ||
            (nextPlatform != null && nextPlatform.triggerTime <= nextStored.triggerAtMillis)

        return listOf(
            AlarmIntegrityCheck("EXACT ALARM", if (exactAlarmReady) "Capability granted" else "Special access required", exactAlarmReady),
            AlarmIntegrityCheck("NOTIFICATIONS", if (notificationsEnabled) "Channel may alert" else "Permission or app notifications blocked", notificationsEnabled),
            AlarmIntegrityCheck("LOCK SCREEN", if (fullScreenReady) "Full-screen signal permitted" else "Full-screen alarm access required", fullScreenReady),
            AlarmIntegrityCheck("ALARM CHANNEL", if (alarmChannelReady) "Channel active" else "Alarm channel disabled", alarmChannelReady),
            AlarmIntegrityCheck("SIGNAL SOURCE", if (alarmToneReady) "Alarm tone available" else "No system tone found", alarmToneReady),
            AlarmIntegrityCheck(
                "PLATFORM LINK",
                when {
                    nextStored == null -> "No EutherTime alarm armed"
                    nextPlatform?.triggerTime == nextStored.triggerAtMillis -> "Android matches stored signal"
                    platformCoversStored -> "Another system alarm occurs first"
                    else -> "Stored signal is not visible as Android's next alarm"
                },
                platformCoversStored,
            ),
        )
    }
}
