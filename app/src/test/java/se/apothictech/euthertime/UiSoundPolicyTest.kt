package se.apothictech.euthertime

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiSoundPolicyTest {
    @Test
    fun `enabled interface audio plays in normal ringer mode`() {
        assertTrue(UiSoundPolicy.shouldPlay(true, AudioManager.RINGER_MODE_NORMAL))
    }

    @Test
    fun `interface audio stays quiet in silent and vibrate modes`() {
        assertFalse(UiSoundPolicy.shouldPlay(true, AudioManager.RINGER_MODE_SILENT))
        assertFalse(UiSoundPolicy.shouldPlay(true, AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test
    fun `disabled interface audio never plays`() {
        assertFalse(UiSoundPolicy.shouldPlay(false, AudioManager.RINGER_MODE_NORMAL))
    }
}
