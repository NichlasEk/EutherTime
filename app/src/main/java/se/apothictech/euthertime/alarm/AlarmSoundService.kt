package se.apothictech.euthertime.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmSoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val volumeHandler = Handler(Looper.getMainLooper())
    private var volumeRamp: Runnable? = null

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
        beginSignal(alarm.stageRole, alarm.soundProfile)
        return START_NOT_STICKY
    }

    private fun beginSignal(role: WakeStageRole, soundProfile: AlarmSoundProfile) {
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

        val started = startMediaPlayer(soundProfile, profile)
        if (!started && soundProfile != AlarmSoundProfile.SYSTEM) {
            startMediaPlayer(AlarmSoundProfile.SYSTEM, profile)
        }

        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(profile.vibrationPattern, 0),
        )
    }

    private fun startMediaPlayer(soundProfile: AlarmSoundProfile, profile: AlarmSignalProfile): Boolean =
        runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                val rawResource = AlarmSoundAssets.rawResourceFor(soundProfile)
                if (rawResource == null) {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(this@AlarmSoundService, uri)
                } else {
                    resources.openRawResourceFd(rawResource).use { descriptor ->
                        setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    }
                }
                isLooping = true
                setVolume(profile.startGain, profile.startGain)
                prepare()
                start()
            }
            mediaPlayer = player
            startVolumeRamp(profile)
        }.isSuccess

    private fun startVolumeRamp(profile: AlarmSignalProfile) {
        val startedAt = SystemClock.elapsedRealtime()
        volumeRamp = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                val gain = profile.gainAt(elapsed)
                player.runCatching { setVolume(gain, gain) }
                if (elapsed < profile.rampDurationMillis) volumeHandler.postDelayed(this, 500L)
            }
        }.also(volumeHandler::post)
    }

    override fun onDestroy() {
        volumeRamp?.let(volumeHandler::removeCallbacks)
        volumeRamp = null
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
