package com.itg.template.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.StringRes
import com.ads.nkh.admob.Admob
import com.ads.nkh.admob.AppOpenManager
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.application.AdsMultiDexApplication
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.config.AdjustConfig
import com.ads.nkh.config.NkhAdConfig
import com.google.android.gms.ads.MobileAds
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

    val ACTION_OPEN_UNINSTALL = "action_open_uninstall"

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GlobalApp
    }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}

        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        initAdRemoteConfig()
        initAds()
        //Option if request need to update
        initShortCut()
    }


    private fun initAdRemoteConfig() {
        AdRemoteConfig.initializeFromAssets(this)
    }

    private fun initAds() {
        val environment =
            if (BuildConfig.DEBUG) NkhAdConfig.ENVIRONMENT_DEVELOP else NkhAdConfig.ENVIRONMENT_PRODUCTION
        mNkhAdConfig = NkhAdConfig(this, environment)

        // Optional: setup Adjust event
        val adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
        mNkhAdConfig.adjustConfig = adjustConfig
        mNkhAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
        mNkhAdConfig.adjustTokenTiktok = resources.getString(R.string.event_token)
        mNkhAdConfig.intervalInterstitialAd = 35

        // Optional: enable ads resume
        mNkhAdConfig.idAdResume = ""

        NkhAd.getInstance().init(this, mNkhAdConfig)

        // Auto disable ad resume after user click ads and back to app
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        // If true -> onNextAction() is called right after Ad Interstitial showed
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(OnBoardingActivity::class.java)
        NkhAd.getInstance().prepareLoadingAdsDialogLayout  = R.layout.layout_prepare_ads
        NkhAd.getInstance().resumeLoadingDialogLayout  = R.layout.layout_welcome_back
    }

    private fun initShortCut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val manager = getSystemService(ShortcutManager::class.java)
            try {
                manager.removeAllDynamicShortcuts()
                val uninstallShortCut = ShortcutInfo.Builder(this, ACTION_OPEN_UNINSTALL)
                    .setShortLabel(getSystemLocaleString(R.string.txt_uninstall))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_uninstall))
                    .setIntent(Intent(this, ConfirmUninstallActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = "android.intent.action.SHORTCUT_UNINSTALL_APP"
                        putExtra(AppConstants.FROM_SHORTCUT, ACTION_OPEN_UNINSTALL)
                    })
                    .setRank(1)
                    .build()
                manager.dynamicShortcuts = listOf(uninstallShortCut)
            } catch (_: Exception) {
            }
        }
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