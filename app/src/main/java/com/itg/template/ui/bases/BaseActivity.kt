package com.itg.template.ui.bases

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.ads.module.admob.Admob
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.itg.template.R
import com.itg.template.ads.AdUnitConfig
import com.itg.template.ads.AdsManager
import com.itg.template.app.AppConstants
import com.itg.template.data.pref.AppSharedPref
import com.itg.template.ui.bases.ext.goneView
import com.itg.template.ui.bases.ext.visibleView
import com.itg.template.utils.ITGTrackingHelper
import com.itg.template.utils.Routes
import java.util.Locale
import javax.inject.Inject

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {

    companion object {

        private const val TAG = "BaseActivity"

        private const val DISTANCE_TIME_NEED_CHECK_RELOAD_BANNER = 1000L
    }

    lateinit var mBinding: VB

    // Banner config and isCollapse
    open val bannerConfig: Pair<AdUnitConfig, Boolean> = Pair(AdUnitConfig(id = "", isEnable = false, reloadIntervalSeconds = 0), false)

    private var loadingView: View? = null

    @Inject
    lateinit var appSharedPref: AppSharedPref

    protected open val shouldShowNavigationBars = false

    private var timeNeedReloadBanner = 0L

    private var handlerBanner: Handler? = null

    private var runnableBanner = object : Runnable {

        override fun run() {
            val isNeedReloadBanner = isNeedReloadBanner()
            if (timeNeedReloadBanner < System.currentTimeMillis() && isNeedReloadBanner) {
                loadBanner()
            }
            if (isNeedReloadBanner) handlerBanner?.postDelayed(this, DISTANCE_TIME_NEED_CHECK_RELOAD_BANNER)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocal()

        requestWindow()
        val layoutView = getLayoutActivity()
        mBinding = DataBindingUtil.setContentView(this, layoutView)
        Log.d(TAG, "onCreate: name Class: ${this::class.java.simpleName}")
        ITGTrackingHelper.addScreenTrack(this::class.java.simpleName)
        if (intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM) != null) {
            Routes.addTrackingMoveScreen(
                intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM).toString(),
                this::class.java.simpleName
            )
        }
        mBinding.lifecycleOwner = this
        registerBackPress()
        if (bannerConfig.first.isEnable) {
            handlerBanner = Handler(Looper.getMainLooper())
            loadBanner()
        } else {
            val frAds = findViewById<FrameLayout>(R.id.fr_banner)
            frAds?.goneView()
        }

        initLoadingView()
        initViews()
        onResizeViews()
        onClickViews()
        observerData()
    }

    open fun setUpViews() {}

    abstract fun getLayoutActivity(): Int

    open fun requestWindow() {}

    open fun initViews() {}

    open fun onResizeViews() {}

    open fun onClickViews() {}

    open fun observerData() {}

    private fun setLocal() {
        val languageCode = appSharedPref.languageCode
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }


    override fun onResume() {
        super.onResume()
        setUpSystemBars()
        if (isNeedReloadBanner()) handlerBanner?.postDelayed(runnableBanner, DISTANCE_TIME_NEED_CHECK_RELOAD_BANNER)
    }

    override fun onPause() {
        super.onPause()
        handlerBanner?.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerBanner?.removeCallbacksAndMessages(null)
        handlerBanner = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setUpSystemBars()

    }

    protected fun loadBanner() {
        try {
            val frAds = findViewById<FrameLayout>(R.id.fr_banner)
            if (frAds != null) {
                frAds.visibleView()
                AdsManager.loadBanner(this@BaseActivity, bannerConfig.first, frAds, bannerConfig.second)
                val distanceReloadBanner = bannerConfig.first.reloadIntervalSeconds ?: 0
                if (distanceReloadBanner > 0) {
                    timeNeedReloadBanner = System.currentTimeMillis() + distanceReloadBanner * 1000L
                }
            }
        } catch (_: Exception) {}
    }

    private fun isNeedReloadBanner(): Boolean {
        val distanceReloadBanner = bannerConfig.first.reloadIntervalSeconds ?: 0
        return bannerConfig.first.isEnable && distanceReloadBanner > 0
    }


    private fun registerBackPress() {

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onActivityBackPressed()
            }
        })
    }

    open fun onActivityBackPressed() {
        finish()
    }


    private fun setUpSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { view, insets ->
            if (!shouldShowNavigationBars) {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updatePadding(bottom = navigationBarInsets.bottom)
            }

            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBarInsets.top)

            insets
        }
    }

    protected fun showInterNotCheckGap(interAd: ApInterstitialAd?, func: (() -> Unit)) {
        if (interAd != null) {
            Admob.getInstance()
                .forceShowInterstitial(this, interAd.interstitialAd, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        func.invoke()
                    }
                })
        } else func.invoke()
    }

    protected fun showLoading() {
        if (loadingView == null) {
            initLoadingView()
        }
        loadingView?.isVisible = true
    }

    protected fun hideLoading() {
        loadingView?.isVisible = false
    }

    private fun initLoadingView() {
        val parent: ViewGroup = findViewById(android.R.id.content)
        if (loadingView == null) {
            loadingView = LayoutInflater.from(this).inflate(R.layout.view_loading, parent, false)
            loadingView?.isVisible = false
            parent.addView(loadingView)
        }
    }

}