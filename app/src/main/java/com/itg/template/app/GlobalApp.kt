package com.itg.template.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.billing.AppPurchase
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig
import com.google.android.gms.ads.MobileAds
import com.itg.devconfig.DevConfig
import com.itg.template.BuildConfig
import com.itg.template.R
import com.itg.template.ads.AdRemoteConfig
import com.itg.template.ads.open_resume
import com.itg.template.ui.component.language.LanguageActivity
import com.itg.template.ui.component.onboarding.OnBoardingActivity
import com.itg.template.ui.component.splash.SplashActivity
import com.itg.template.ui.component.uninstall.ConfirmUninstallActivity
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import kotlin.jvm.java

@HiltAndroidApp
class GlobalApp : AdsMultiDexApplication() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GlobalApp

        @SuppressLint("StaticFieldLeak")
        var currentActivity: Activity? = null
    }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        DevConfig.init(
            context = this,
            nkhStudioVersion = BuildConfig.ERAIN_STUDIO_VERSION,
            playServicesAdsVersion = BuildConfig.PLAY_SERVICES_ADS_VERSION,
            gdprModuleVersion = BuildConfig.GDPR_MODULE_VERSION
        )


        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        initAdRemoteConfig()
        initAds()

        // Unconditionally register lifecycle observer and callbacks so dynamic welcome/resume toggling works during testing
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        registerActivityLifecycleCallbacks(AppActivityLifecycleCallbacks())
    }

    private fun initAdRemoteConfig() {
        AdRemoteConfig.initializeFromAssets(this)
    }

    private fun initAds() {
        val environment =
            if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
        mERainAdConfig = ERainAdConfig(this, environment)

        val adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
        mERainAdConfig.adjustConfig = adjustConfig
        mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
        mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.event_token)
        mERainAdConfig.intervalInterstitialAd = 35

        mERainAdConfig.idAdResume = ""

        ERainAd.getInstance().init(this, mERainAdConfig)

        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(OnBoardingActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(ConfirmUninstallActivity::class.java)

    }

    fun Context.getSystemLocaleString(@StringRes resId: Int): String {
        val systemConfig = Resources.getSystem().configuration
        val systemLocale = systemConfig.locales[0]

        val config = Configuration(resources.configuration)
        config.setLocale(systemLocale)

        val systemContext = createConfigurationContext(config)
        return systemContext.getString(resId)
    }
}
