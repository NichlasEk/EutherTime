package se.apothictech.euthertime.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveAlarmSessionPolicyTest {
    @Test
    fun `fresh ringing session remains active`() {
        assertTrue(ActiveAlarmSessionPolicy.isFresh(1_000L, 61_000L))
    }

    @Test
    fun `future and expired sessions are rejected`() {
        assertFalse(ActiveAlarmSessionPolicy.isFresh(2_000L, 1_000L))
        assertFalse(
            ActiveAlarmSessionPolicy.isFresh(
                1_000L,
                1_000L + ActiveAlarmSessionPolicy.MAX_ACTIVE_MILLIS + 1L,
            ),
        )
    }

    @Test
    fun `missing start timestamp is rejected`() {
        assertFalse(ActiveAlarmSessionPolicy.isFresh(0L, 1_000L))
    }
}
