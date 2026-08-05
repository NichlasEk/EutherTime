package se.apothictech.euthertime

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes

internal enum class UiSoundCue(@RawRes val resourceId: Int, val volume: Float) {
    TAP(R.raw.euthertime_ui_tap, 0.65f),
    SELECT(R.raw.euthertime_ui_select, 0.75f),
    CONFIRM(R.raw.euthertime_ui_confirm, 0.90f),
    ERROR(R.raw.euthertime_ui_error, 0.75f),
}

internal object UiSoundPolicy {
    fun shouldPlay(enabled: Boolean, ringerMode: Int): Boolean =
        enabled && ringerMode == AudioManager.RINGER_MODE_NORMAL
}

internal object UiSoundPreferences {
    private const val STORE = "euthertime_interface_audio"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}

internal class UiSoundEngine(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundIds = UiSoundCue.entries.associateWith { soundPool.load(context, it.resourceId, 1) }

    fun play(cue: UiSoundCue, enabled: Boolean): Boolean {
        if (!UiSoundPolicy.shouldPlay(enabled, audioManager.ringerMode)) return false
        val soundId = soundIds.getValue(cue)
        return soundPool.play(soundId, cue.volume, cue.volume, 1, 0, 1f) != 0
    }

    fun release() {
        soundPool.release()
    }
}
