package com.itg.template.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.ads.wrapper.ApRewardAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdType
import com.ads.module.funtion.RewardCallback
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.itg.template.ui.bases.ext.goneView
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AdsManager {

    val nativeLanguageAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdLive = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFullLive = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFull2Live = MutableLiveData<ApNativeAd?>()
    val nativeSurveyAdLive = MutableLiveData<ApNativeAd?>()
    val nativeConfirmUninstallAdLive = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativePermissionAdLive = MutableLiveData<ApNativeAd?>()
    val nativeHomeAdLive = MutableLiveData<ApNativeAd?>()
    var interWelcomeAdLive = MutableLiveData<ApInterstitialAd?>()
    // Auto-resolve config for each loaded native ad
    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()
    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    private var interSplashAd: ApInterstitialAd? = null
    private var interOnboarding: ApInterstitialAd? = null

    private var rewardExample :RewardedAd? = null

    private const val DEFAULT_OPEN_SPLASH_TIMEOUT_MS = 15_000L
    // Same timings as the single-unit inter splash in SplashActivity.
    private const val DEFAULT_INTER_SPLASH_TIMEOUT_MS = 30_000L
    private const val INTER_SPLASH_MIN_DELAY_MS = 5_000L
    private const val SPLASH_AD_RESUME_CHECK_DELAY_MS = 1000

    // Splash ad (app open or high floor inter) in progress.
    private var splashAdListener: AdCallback? = null
    private var splashAdCheckOnResume: ((AppCompatActivity, AdCallback) -> Unit)? = null
    private val splashAdShowCallback = object : AdCallback() {
        override fun onNextAction() {
            super.onNextAction()
            completeSplashAd()
        }

        override fun onAdClicked() {
            super.onAdClicked()
            // Keeps app open resume from covering the splash ad when the user comes back.
            AppOpenManager.getInstance().setDisableAdResumeByClickAction(true)
        }
    }

    private fun loadNativeInternal(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
        shouldDisplay: Boolean = true,
    ) {
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
            || !shouldDisplay
        ) {
            liveData.postValue(null)
            return
        }
        ERainAd.getInstance()
            .loadNativeAdResultCallback(activity, config.id, layoutRes, object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    adConfigMap[nativeAd] = config
                    liveData.postValue(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    liveData.postValue(null)
                }
            })
    }

    /**
     * Tries [highConfig]'s unit first and falls back to [loadNativeInternal] with [config],
     * which stays the placement's switch and supplies the CTA style.
     */
    private fun loadNativeHighFloor(
        activity: Activity,
        highConfig: AdUnitConfig,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
    ) {
        val highId = highConfig.id
        if (!highConfig.isEnable
            || highId.isBlank()
            || !config.isEnable
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
        ) {
            loadNativeInternal(activity, config, layoutRes, liveData)
            return
        }
        ERainAd.getInstance()
            .loadNativeAdResultCallback(activity, highId, layoutRes, object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    adConfigMap[nativeAd] = config
                    liveData.postValue(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    Timber.d("Native high floor %s failed, falling back to %s", highId, config.id)
                    loadNativeInternal(activity, config, layoutRes, liveData)
                }
            })
    }

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1 else AdRemoteConfig.native_language_2
        val highConfig =
            if (isFirst) AdRemoteConfig.native_language_1_high else AdRemoteConfig.native_language_2_high
        loadNativeHighFloor(
            activity,
            highConfig,
            config,
            layoutRes,
            nativeLanguageAdLive
        )
    }

    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1_click else AdRemoteConfig.native_language_2_click
        val highConfig =
            if (isFirst) AdRemoteConfig.native_language_1_click_high else AdRemoteConfig.native_language_2_click_high
        loadNativeHighFloor(
            activity,
            highConfig,
            config,
            layoutRes,
            nativeLanguageClickAdLive
        )
    }

    fun isOpenSplashEnabled(): Boolean = AdRemoteConfig.open_splash.isEnable

    /** Without a usable high floor unit the inter splash keeps its single-unit flow. */
    fun isInterSplashHighFloorEnabled(): Boolean {
        val highConfig = AdRemoteConfig.inter_splash_high
        return highConfig.isEnable && highConfig.id.isNotBlank()
    }

    fun loadOpenSplash(activity: AppCompatActivity, callback: AdCallback) {
        val config = AdRemoteConfig.open_splash
        loadSplashAd(
            activity,
            callback,
            config,
            AdRemoteConfig.open_splash_high,
            timeoutMs = config.timeoutMs ?: DEFAULT_OPEN_SPLASH_TIMEOUT_MS,
            load = { ids, timeoutMs, loadCallback ->
                AppOpenManager.getInstance()
                    .loadAndShowOpenSplashSequentially(activity, ids, timeoutMs, loadCallback)
            },
            show = { showActivity, showCallback ->
                AppOpenManager.getInstance().showOpenSplashSequentially(showActivity, showCallback)
            },
            checkOnResume = { resumedActivity, showCallback ->
                AppOpenManager.getInstance().onCheckShowOpenSplashSequentiallyWhenFail(
                    resumedActivity, showCallback, SPLASH_AD_RESUME_CHECK_DELAY_MS
                )
            }
        )
    }

    fun loadHighFloorInterSplash(activity: AppCompatActivity, callback: AdCallback) {
        val config = AdRemoteConfig.inter_splash
        loadSplashAd(
            activity,
            callback,
            config,
            AdRemoteConfig.inter_splash_high,
            timeoutMs = config.timeoutMs ?: DEFAULT_INTER_SPLASH_TIMEOUT_MS,
            load = { ids, timeoutMs, loadCallback ->
                ERainAd.getInstance().loadInterSplashPrioritySequentially(
                    activity, ids, timeoutMs, INTER_SPLASH_MIN_DELAY_MS, loadCallback
                )
            },
            show = { showActivity, showCallback ->
                ERainAd.getInstance().onShowSplashPrioritySequentially(showActivity, showCallback)
            },
            checkOnResume = { resumedActivity, showCallback ->
                ERainAd.getInstance().onCheckShowSplashPrioritySequentiallyWhenFail(
                    resumedActivity, showCallback, SPLASH_AD_RESUME_CHECK_DELAY_MS
                )
            }
        )
    }

    /**
     * Loads [highConfig]'s unit first and [config]'s unit only after it fails, then shows the ad
     * through [show]. An ad that is ready while the splash is in the background is kept by the SDK
     * and shown by [checkOnResume]; [callback] gets `onAdLoaded` when an ad is ready and
     * `onNextAction` exactly once.
     */
    private fun loadSplashAd(
        activity: AppCompatActivity,
        callback: AdCallback,
        config: AdUnitConfig,
        highConfig: AdUnitConfig,
        timeoutMs: Long,
        load: (ids: List<String>, timeoutMs: Long, loadCallback: AdCallback) -> Unit,
        show: (activity: AppCompatActivity, showCallback: AdCallback) -> Unit,
        checkOnResume: (activity: AppCompatActivity, showCallback: AdCallback) -> Unit,
    ) {
        releaseSplashAd()
        val ids = listOf(highConfig, config)
            .filter { it.isEnable && it.id.isNotBlank() }
            .map { it.id }
            .distinct()
        if (!config.isEnable
            || ids.isEmpty()
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
        ) {
            callback.onNextAction()
            return
        }
        splashAdListener = callback
        splashAdCheckOnResume = checkOnResume
        load(ids, timeoutMs, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                Log.d("Newads", "onAdSplashReady")
                val listener = splashAdListener ?: return
                listener.onAdLoaded()
                // In background the SDK keeps the ad for checkOnResume instead of showing it.
                show(activity, splashAdShowCallback)
            }

            override fun onNextAction() {
                super.onNextAction()
                Log.d("Newads", "onNextAction")
                completeSplashAd()
            }
        })
    }

    /** Call from the splash `onResume`: shows an ad the SDK kept while the app was in background. */
    fun onSplashAdResumed(activity: AppCompatActivity) {
        splashAdCheckOnResume?.invoke(activity, splashAdShowCallback)
    }

    /** Call from the splash `onDestroy`; later SDK callbacks are ignored. */
    fun releaseSplashAd() {
        splashAdListener = null
        splashAdCheckOnResume = null
    }

    // Both SDK show calls end with onNextAction, after the ad closes or fails to show.
    private fun completeSplashAd() {
        val callback = splashAdListener ?: return
        releaseSplashAd()
        callback.onNextAction()
    }

    /** Points app open resume at the high floor unit first, falling back to the normal one. */
    fun applyOpenResumeAdIds() {
        val highConfig = AdRemoteConfig.open_resume_high
        AppOpenManager.getInstance().setAppResumeAdId(
            highConfig.id.takeIf { highConfig.isEnable && it.isNotBlank() },
            AdRemoteConfig.open_resume.id
        )
    }

    fun loadNativeOnboarding1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_1 else AdRemoteConfig.native_onboarding_2_1
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding1AdLive
        )
    }

    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_4 else AdRemoteConfig.native_onboarding_2_4
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding4AdLive,
            ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2(config.enableUaCheck)
        )
    }

    fun loadNativeOnboardingFull(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_3 else AdRemoteConfig.native_onboarding_fullscreen_2_3
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeAdOnBoardingFullLive,
            ERainAd.getInstance().getShouldDisplayNativeOnboardingFull1(config.enableUaCheck)
        )
    }

    fun loadNativeOnboardingFull2(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_4 else AdRemoteConfig.native_onboarding_fullscreen_2_4
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeAdOnBoardingFull2Live,
            ERainAd.getInstance().getShouldDisplayNativeOnboardingFull2(config.enableUaCheck)
        )
    }

    fun loadNativePermission(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.native_permission
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativePermissionAdLive,
            ERainAd.getInstance().getShouldDisplayNativePermission(config.enableUaCheck)
            )
    }

    fun loadNativeHome(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.native_home
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeHomeAdLive,
            ERainAd.getInstance().getShouldDisplayNativeHome(config.enableUaCheck)
        )
    }

    fun loadNativeSurvey(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_survey, layoutRes, nativeSurveyAdLive,
            ERainAd.getInstance()
                .getShouldDisplayWidgetUninstall(AdRemoteConfig.native_survey.enableUaCheck)
        )
    }

    fun loadNativeConfirmUninstall(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_confirm_uninstall,
            layoutRes,
            nativeConfirmUninstallAdLive,
            ERainAd.getInstance()
                .getShouldDisplayWidgetUninstall(AdRemoteConfig.native_confirm_uninstall.enableUaCheck)
        )
    }

    fun loadNativeWelcome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_welcome,
            layoutRes,
            nativeWelcomeAdLive,
            ERainAd.getInstance().getShouldDisplayNativeWelcomeBack(AdRemoteConfig.native_welcome.enableUaCheck)
        )
    }

    // ── Dashboard / Test helpers (ignore shouldDisplay) ──

    /** Dedicated LiveData for customization preview – won't collide with real flows */
    val nativeDashboardPreviewLive = MutableLiveData<ApNativeAd?>()

    /**
     * Load a native ad for dashboard preview purposes.
     * Bypasses all shouldDisplay checks so it always loads.
     */
    fun loadNativeForDashboard(activity: Activity, configKey: String, layoutRes: Int) {
        val config = try {
            AdRemoteConfig.getInstance().ads[configKey]
                ?: AdUnitConfig(id = "", isEnable = false)
        } catch (_: Exception) {
            AdUnitConfig(id = "", isEnable = false)
        }
        // Force shouldDisplay = true to bypass SDK limits
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeDashboardPreviewLive,
            shouldDisplay = true
        )
    }

    /** Load native language ad for dashboard – ignores shouldDisplay */
    fun loadNativeLanguageForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_language_1", layoutRes)
    }

    /** Load native onboarding full for dashboard – ignores shouldDisplay */
    fun loadNativeFullForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_onboarding_fullscreen_1_3", layoutRes)
    }

    fun loadInterOnboarding(context: Context, ignoreLimit: Boolean = false) {
        val config = AdRemoteConfig.inter_onboarding
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !ERainAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck))
        ) {
            interOnboarding = null
            return
        }
        interOnboarding =
            ERainAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterOnboarding(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && (ignoreLimit)
        ) {
            ERainAd.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onAction()
                    }
                }, true)
        } else {
            onAction()
        }
    }

    fun loadInterWelcome(context: Context, ignoreLimit: Boolean = false) {
        val cached = interWelcomeAdLive.value?.takeIf { it.isReady }
        val request = MutableLiveData<ApInterstitialAd?>()
        interWelcomeAdLive = request
        fun emit(ad: ApInterstitialAd?) {
            request.value = ad
        }
        val config = AdRemoteConfig.inter_welcome
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !ERainAd.getInstance()
                .getShouldDisplayInterWelcomeBack(config.enableUaCheck))
        ) {
            emit(null)
            return
        }
        if (cached != null) {
            emit(cached)
            return
        }
        var loaded: ApInterstitialAd? = null
        loaded = ERainAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {
            override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                super.onApInterstitialLoad(apInterstitialAd)
                emit(apInterstitialAd ?: loaded)
            }

            override fun onAdFailedToLoad(adError: LoadAdError?) {
                super.onAdFailedToLoad(adError)
                emit(null)
            }
        })
        if (loaded?.isReady == true) emit(loaded)
    }

    fun showInterWelcome(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interWelcomeAdLive.value
        val allowShow = ignoreLimit || ERainAd.getInstance()
            .getShouldDisplayInterWelcomeBack(AdRemoteConfig.inter_welcome.enableUaCheck)
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && allowShow
        ) {
            ERainAd.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        interWelcomeAdLive = MutableLiveData()
                        onAction()
                    }
                }, false)
        } else {
            onAction()
        }
    }

    fun loadAndShowReward(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (AdRemoteConfig.reward_example.isEnable.not() || AppPurchase.getInstance()
                .isPurchased(activity)
        ) {
            onFailed()
            return
        }

        ERainAd.getInstance().initRewardAds(
            activity,
            AdRemoteConfig.reward_example.id,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    rewardExample = rewardedAd

                    var isEarn = false
                    ERainAd.getInstance().showRewardAds(
                        activity,
                        rewardExample,
                        object : RewardCallback {
                            override fun onUserEarnedReward(var1: RewardItem?) {
                                isEarn = true
                                rewardExample = null
                            }

                            override fun onRewardedAdClosed() {
                                if (isEarn) onSuccess()
                                else onFailed()
                            }

                            override fun onRewardedAdFailedToShow(codeError: Int) {
                                rewardExample = null
                                onFailed()
                            }

                            override fun onAdClicked() {

                            }
                        }
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    onFailed()
                }
            }
        )
    }

    fun loadBanner(
        activity: AppCompatActivity,
        adUnitConfig: AdUnitConfig,
        frAds: FrameLayout,
        isCollapse: Boolean,
    ) {
        if (adUnitConfig.isEnable) {
            removeBannerView(activity, frAds)
            if (isCollapse) ERainAd.getInstance().loadCollapsibleBanner(
                activity,
                adUnitConfig.id,
                AppConstant.CollapsibleGravity.BOTTOM,
                object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
            else ERainAd.getInstance()
                .loadBanner(activity, adUnitConfig.id, object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
        } else {
            frAds.removeAllViews()
            frAds.goneView()
        }
    }

    @SuppressLint("InflateParams")
    private fun removeBannerView(activity: Activity, frAds: FrameLayout) {
        try {
            val container = frAds.findViewById<FrameLayout>(com.ads.module.R.id.banner_container)
            if (container != null) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is AdView) {
                        view.destroy()
                        container.removeView(view)
                    }
                }
            }
            val shimmerFrameLayout = LayoutInflater.from(activity)
                .inflate(com.ads.module.R.layout.layout_banner_control, null)
            frAds.removeAllViews()
            frAds.addView(shimmerFrameLayout)
        } catch (_: Exception) {
        }
    }

    fun clearAll() {
        nativeLanguageAdLive.postValue(null)
        nativeLanguageClickAdLive.postValue(null)
        nativeOnboarding1AdLive.postValue(null)
        nativeOnboarding4AdLive.postValue(null)
        nativeAdOnBoardingFullLive.postValue(null)
        nativeSurveyAdLive.postValue(null)
        nativeConfirmUninstallAdLive.postValue(null)
        nativeWelcomeAdLive.postValue(null)
        interWelcomeAdLive = MutableLiveData()
        interSplashAd = null
        interOnboarding = null
    }

    private fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
}
