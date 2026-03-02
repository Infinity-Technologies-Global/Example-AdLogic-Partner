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
    private var preLoadNativeListener: PreLoadNativeListener? = null

    fun setPreLoadNativeCallback(listener: PreLoadNativeListener) {
        preLoadNativeListener = listener
    }

    var nativeLanguageAd: ApNativeAd? = null
        private set

    var nativeLanguageClickAd: ApNativeAd? = null
        private set

    var nativeOnboarding1Ad: ApNativeAd? = null
        private set

    var nativeOnboarding4Ad: ApNativeAd? = null
        private set

    var nativeAdOnBoardingFull: ApNativeAd? = null
        private set

    private var interSplashAd: ApInterstitialAd? = null

    private var interOnboarding: ApInterstitialAd? = null

    private fun loadNativeConfig(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        onLoaded: (ApNativeAd) -> Unit,
    ) {
        if (!config.isEnable || AppPurchase.getInstance()
                .isPurchased(activity) || !activity.isNetworkAvailable()
        ) {
            preLoadNativeListener?.onLoadNativeFail()
            return
        }
        ERainAd.getInstance()
            .loadNativeAdResultCallback(activity, config.id, layoutRes, object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    onLoaded(nativeAd)
                    preLoadNativeListener?.onLoadNativeSuccess()
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    preLoadNativeListener?.onLoadNativeFail()
                }
            })
    }

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val mainConfig =
            if (isFirst) AdRemoteConfig.native_language_1 else AdRemoteConfig.native_language_2
        loadNativeConfig(activity, mainConfig, layoutRes) { nativeAd ->
            nativeLanguageAd = nativeAd
        }
    }
    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val clickConfig =
            if (isFirst) AdRemoteConfig.native_language_1_click else AdRemoteConfig.native_language_2_click
        loadNativeConfig(activity, clickConfig, layoutRes) { nativeAd ->
            nativeLanguageClickAd = nativeAd
        }
    }

    fun loadNativeOnboarding1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val onboarding1Config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_1 else AdRemoteConfig.native_onboarding_2_1
        loadNativeConfig(activity, onboarding1Config, layoutRes) { nativeAd ->
            nativeOnboarding1Ad = nativeAd
        }

    }
    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val onboarding4Config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_4 else AdRemoteConfig.native_onboarding_2_4
        loadNativeConfig(activity, onboarding4Config, layoutRes) { nativeAd ->
            nativeOnboarding4Ad = nativeAd
        }

    }

    fun loadNativeOnboardingFull(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val onboardingFullConfig =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_3 else AdRemoteConfig.native_onboarding_fullscreen_2_3
        loadNativeConfig(activity, onboardingFullConfig, layoutRes) { nativeAd ->
            nativeAdOnBoardingFull = nativeAd
        }
    }

    fun loadInterOnboarding(context: Context) {
        val config = AdRemoteConfig.inter_onboarding
        if (!config.isEnable || AppPurchase.getInstance().isPurchased(context)) {
            interOnboarding = null
            return
        }
        interOnboarding =
            ERainAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterOnboarding(context: Context, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context)
        ) {
            ERainAd.getInstance().forceShowInterstitial(context, interstitial, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, true)
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
                object : com.ads.module.funtion.AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
            else ERainAd.getInstance()
                .loadBanner(activity, adUnitConfig.id, object : com.ads.module.funtion.AdCallback() {
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
        nativeLanguageAd = null
        nativeLanguageClickAd = null
        nativeOnboarding1Ad = null
        nativeOnboarding4Ad = null
        nativeAdOnBoardingFull = null
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
