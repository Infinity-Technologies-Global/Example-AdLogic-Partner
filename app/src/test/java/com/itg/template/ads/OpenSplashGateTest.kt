package com.itg.template.ads

import com.itg.template.ads.OpenSplashGate.ReadyAction
import com.itg.template.ads.OpenSplashGate.ResumeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSplashGateTest {

    private val timeout = 15_000L
    private val gate = OpenSplashGate(startAtMs = 0L, timeoutMs = timeout)

    @Test
    fun onAdReady_showsFirstAdInForeground() {
        assertEquals(ReadyAction.SHOW, gate.onAdReady(2_000L, isForeground = true))
    }

    @Test
    fun onAdReady_ignoresSecondAdWhileShowing() {
        gate.onAdReady(2_000L, isForeground = true)

        assertEquals(ReadyAction.IGNORE, gate.onAdReady(3_000L, isForeground = true))
    }

    @Test
    fun onAdReady_showsAdReportedAtTimeout() {
        assertEquals(ReadyAction.SHOW, gate.onAdReady(timeout, isForeground = true))
    }

    @Test
    fun onAdReady_ignoresAdAfterTimeout() {
        val late = timeout + OpenSplashGate.READY_GRACE_MS + 1

        assertEquals(ReadyAction.IGNORE, gate.onAdReady(late, isForeground = true))
    }

    @Test
    fun onAdReady_ignoresAdAfterFinish() {
        gate.giveUp()

        assertEquals(ReadyAction.IGNORE, gate.onAdReady(2_000L, isForeground = true))
    }

    @Test
    fun onResumed_showsAdDeferredInBackground() {
        assertEquals(ReadyAction.DEFER, gate.onAdReady(2_000L, isForeground = false))

        assertEquals(ResumeAction.SHOW, gate.onResumed(5_000L))
    }

    @Test
    fun onResumed_finishesWhenDeferredAdExpired() {
        gate.onAdReady(2_000L, isForeground = false)

        assertEquals(ResumeAction.FINISH, gate.onResumed(timeout + 5_000L))
    }

    @Test
    fun onResumed_doesNothingWithoutPendingAd() {
        assertEquals(ResumeAction.NONE, gate.onResumed(2_000L))
    }

    @Test
    fun giveUp_keepsSplashWhileAdShowing() {
        gate.onAdReady(2_000L, isForeground = true)

        assertFalse(gate.giveUp())
    }

    @Test
    fun giveUp_finishesWhenNoAdShowing() {
        assertTrue(gate.giveUp())
    }

    @Test
    fun finish_succeedsOnlyOnce() {
        assertTrue(gate.finish())
        assertFalse(gate.finish())
        assertFalse(gate.giveUp())
    }
}
