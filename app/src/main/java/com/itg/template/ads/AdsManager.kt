package com.itg.template.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.itg.template.ui.bases.ext.goneView
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AdsManager {

    val nativeLanguageAdLive            = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive       = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdLive         = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdLive         = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFullLive      = MutableLiveData<ApNativeAd?>()
    val nativeSurveyAdLive              = MutableLiveData<ApNativeAd?>()
    val nativeConfirmUninstallAdLive    = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdLive             = MutableLiveData<ApNativeAd?>()

    // Auto-resolve config for each loaded native ad
    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()
    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    private var interSplashAd: ApInterstitialAd? = null
    private var interOnboarding: ApInterstitialAd? = null
    private var interWelcomeAd: ApInterstitialAd? = null

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

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config = if (isFirst) AdRemoteConfig.native_language_1 else AdRemoteConfig.native_language_2
        loadNativeInternal(activity, config, layoutRes, nativeLanguageAdLive)
    }

    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config = if (isFirst) AdRemoteConfig.native_language_1_click else AdRemoteConfig.native_language_2_click
        loadNativeInternal(activity, config, layoutRes, nativeLanguageClickAdLive)
    }

    fun loadNativeOnboarding1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config = if (isFirst) AdRemoteConfig.native_onboarding_1_1 else AdRemoteConfig.native_onboarding_2_1
        loadNativeInternal(activity, config, layoutRes, nativeOnboarding1AdLive, ERainAd.getInstance().shouldDisplayNativeOnboardingNormal1)
    }

    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config = if (isFirst) AdRemoteConfig.native_onboarding_1_4 else AdRemoteConfig.native_onboarding_2_4
        loadNativeInternal(activity, config, layoutRes, nativeOnboarding4AdLive, ERainAd.getInstance().shouldDisplayNativeOnboardingNormal2)
    }

    fun loadNativeOnboardingFull(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config = if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_3 else AdRemoteConfig.native_onboarding_fullscreen_2_3
        loadNativeInternal(activity, config, layoutRes, nativeAdOnBoardingFullLive, ERainAd.getInstance().shouldDisplayNativeOnboardingFull1)
    }

    fun loadNativeSurvey(activity: Activity, layoutRes: Int) {
        loadNativeInternal(activity, AdRemoteConfig.native_survey, layoutRes, nativeSurveyAdLive)
    }

    fun loadNativeConfirmUninstall(activity: Activity, layoutRes: Int) {
        loadNativeInternal(activity, AdRemoteConfig.native_confirm_uninstall, layoutRes, nativeConfirmUninstallAdLive)
    }

    fun loadNativeWelcome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(activity, AdRemoteConfig.native_welcome, layoutRes, nativeWelcomeAdLive,
            ERainAd.getInstance().shouldDisplayNativeWelcomeBack)
    }

    fun loadInterOnboarding(context: Context) {
        val config = AdRemoteConfig.inter_onboarding
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || !ERainAd.getInstance().shouldDisplayInterOnboarding
        ) {
            interOnboarding = null
            return
        }
        interOnboarding =
            ERainAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterOnboarding(context: Context, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && ERainAd.getInstance().shouldDisplayInterOnboarding
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

    fun loadInterWelcome(context: Context) {
        val config = AdRemoteConfig.inter_welcome
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || ERainAd.getInstance().shouldDisplayInterWelcomeBack
        ) {
            interWelcomeAd = null
            return
        }
        interWelcomeAd =
            ERainAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterWelcome(context: Context, onAction: () -> Unit) {
        val interstitial = interWelcomeAd
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context)
        ) {
            ERainAd.getInstance().forceShowInterstitial(context, interstitial, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, false)
        } else {
            onAction()
        }
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
        interSplashAd = null
        interOnboarding = null
        interWelcomeAd = null
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
