package com.itg.template.ads

/**
 * Show/finish rules of the high-floor open splash, kept free of SDK and Android types so they
 * can be unit tested: an ad is shown only before the deadline, at most once, and the splash
 * finishes exactly once.
 */
class OpenSplashGate(private val startAtMs: Long, private val timeoutMs: Long) {

    enum class ReadyAction { SHOW, DEFER, IGNORE }

    enum class ResumeAction { SHOW, FINISH, NONE }

    private var isFinished = false
    private var isShowing = false
    private var hasPendingAd = false

    fun onAdReady(nowMs: Long, isForeground: Boolean): ReadyAction = when {
        isFinished || isShowing || isExpired(nowMs) -> ReadyAction.IGNORE
        !isForeground -> {
            hasPendingAd = true
            ReadyAction.DEFER
        }
        else -> {
            isShowing = true
            ReadyAction.SHOW
        }
    }

    fun onResumed(nowMs: Long): ResumeAction {
        if (isFinished || isShowing || !hasPendingAd) return ResumeAction.NONE
        hasPendingAd = false
        if (isExpired(nowMs)) return ResumeAction.FINISH
        isShowing = true
        return ResumeAction.SHOW
    }

    /** Ends the splash unless an ad is on screen; that ad finishes it when it closes. */
    fun giveUp(): Boolean = !isShowing && finish()

    /** True only on the first call, so the next screen is opened once. */
    fun finish(): Boolean {
        if (isFinished) return false
        isFinished = true
        return true
    }

    private fun isExpired(nowMs: Long) = nowMs - startAtMs > timeoutMs + READY_GRACE_MS

    companion object {
        // An ad reported right at the timeout may reach us a little late, so allow a small margin.
        const val READY_GRACE_MS = 300L
    }
}
