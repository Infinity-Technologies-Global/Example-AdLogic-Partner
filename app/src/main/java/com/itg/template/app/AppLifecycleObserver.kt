package com.itg.template.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.module.admob.AppOpenManager
import com.ads.module.billing.AppPurchase
import com.itg.template.ui.component.splash.SplashActivity
import com.itg.template.ui.component.uninstall.SurveyActivity
import com.itg.template.ui.component.welcome.WelcomeActivity
import com.itg.template.utils.Routes

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val listActivityDisableResume = arrayListOf(
        // Language / Onboarding are not listed: users returning from background on those
        // screens also get the Welcome Back screen.
        SplashActivity::class.java,
        WelcomeActivity::class.java,
        SurveyActivity::class.java,
    )

    override fun onStart(owner: LifecycleOwner) {
        val currentActivity = GlobalApp.currentActivity
        if (currentActivity != null) {
            val isDisable = listActivityDisableResume.any { clazz ->
                clazz.isInstance(currentActivity)
            }
            if (!isDisable && ResumeAdsEntryRule.shouldShowWelcomeOnResume()
                && !AppOpenManager.getInstance().isInterstitialShowing
                && !AppPurchase.getInstance().isPurchased(currentActivity.applicationContext)
            ) {
                if (AppOpenManager.getInstance().isDisableAdResumeByClickAction)
                    AppOpenManager.getInstance().isDisableAdResumeByClickAction = false
                else
                    Routes.startWelcomeActivity(currentActivity)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {}
}
