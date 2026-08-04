package se.apothictech.euthertime.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmSoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        val stored = AlarmStore.get(this, id)
        val kind = runCatching {
            AlarmKind.valueOf(intent?.getStringExtra(AlarmScheduler.EXTRA_KIND).orEmpty())
        }.getOrDefault(stored?.kind ?: AlarmKind.ALARM)
        val alarm = stored ?: ScheduledAlarm(
            id = id,
            triggerAtMillis = System.currentTimeMillis(),
            label = intent?.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "EutherTime",
            kind = kind,
        )

        startForeground(AlarmNotifications.notificationId(alarm.id), AlarmNotifications.build(this, alarm))
        beginSignal(alarm.stageRole)
        return START_NOT_STICKY
    }

    private fun beginSignal(role: WakeStageRole) {
        if (mediaPlayer?.isPlaying == true) return
        val profile = AlarmSignalProfiles.forRole(role)

        val audioManager = getSystemService(AudioManager::class.java)
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { }
            .build()
        audioManager.requestAudioFocus(audioFocusRequest!!)

        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(this@AlarmSoundService, uri)
                isLooping = true
                setVolume(profile.gain, profile.gain)
                prepare()
                start()
            }
        }

        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(profile.vibrationPattern, 0),
        )
    }

    override fun onDestroy() {
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        audioFocusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
