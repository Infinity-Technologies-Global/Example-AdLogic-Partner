package com.itg.template.ui.component.language

import android.os.Handler
import android.os.Looper
import com.ads.module.ads.wrapper.ApNativeAd
import com.itg.devconfig.utils.setOnAdminAdToggleListener
import com.itg.template.R
import com.itg.template.ads.AdsManager
import com.itg.template.ads.AdsManager.loadNativeLanguageClick
import com.itg.template.ads.RemoteConfigUtils
import com.itg.template.ads.populateNativeAdView
import com.itg.template.app.AppConstants
import com.itg.template.app.AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
import com.itg.template.databinding.ActivityLanguageBinding
import com.itg.template.ui.bases.BaseActivity
import com.itg.template.ui.bases.ext.click
import com.itg.template.ui.bases.ext.goneView
import com.itg.template.ui.bases.ext.isNetwork
import com.itg.template.ui.bases.ext.visibleView
import com.itg.template.ui.component.language.adapter.LanguageAdapter
import com.itg.template.ui.component.language.data.LanguageData
import com.itg.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private var timeDelayDoneButton = DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
    private var isFromSetting = false
    override val shouldShowNavigationBars = RemoteConfigUtils.getOnShowNavigationButton()

    companion object {
        const val EXTRA_FROM_SETTING = "extra_from_setting"
    }

    override fun getLayoutActivity() = R.layout.activity_language

    private val fromSetting
        get() = intent.getBooleanExtra(AppConstants.KEY_SETTING, false)

    private var shouldDelayDoneButton = true
    private var selectedIso = LanguageData.defaultLanguage.iso

    private val languageAdapter: LanguageAdapter by lazy {
        LanguageAdapter(
            onItemLanguageClick = {
                listenLanguageClickAd()
                delayShowDoneButton()
                selectedIso = it.iso
                LanguageData.selectLanguage(it.iso)
                resubmitLanguageData()
            }
        )
    }

    override fun initViews() {
        isFromSetting = intent.getBooleanExtra(EXTRA_FROM_SETTING, false)
        mBinding.tvTitle.setOnAdminAdToggleListener(){
            Routes.startSplashActivity(this@LanguageActivity)
            finish()
        }
        shouldDelayDoneButton = RemoteConfigUtils.shouldDelayLanguageDoneButton()
        timeDelayDoneButton = RemoteConfigUtils.getTimeDelayButtonDoneLanguage()
        initAdapter()
        initLayout()

        mBinding.root.postDelayed({
            loadNativeLanguageClick(this, appSharedPref.firstLanguage, R.layout.layout_native_language_click)
            initAds()
        }, 100L)
    }

    override fun observerData() {
        listenLanguageAd()
    }

    override fun onClickViews() {
        mBinding.ivDone.click {
            val iso = selectedIso
            appSharedPref.languageCode = iso
            startNextActivity()
        }
        mBinding.tvDone.click {
            mBinding.ivDone.performClick()
        }
    }

    private fun initAdapter() {
        mBinding.recyclerView.adapter = languageAdapter
        resubmitLanguageData()
    }

    private fun initLayout() {
        if (isFromSetting) {
            mBinding.tvDone.visibleView()
            mBinding.ivDone.visibleView()
            timeDelayDoneButton = 0
        } else {
            if (shouldDelayDoneButton) {
                mBinding.ivDone.goneView()
                mBinding.tvDone.goneView()
            } else {
                mBinding.ivDone.visibleView()
                mBinding.tvDone.visibleView()
            }
        }
    }

    private fun initAds() {
        if (fromSetting) {
            mBinding.flAds.goneView()
        } else {
            AdsManager.loadNativeOnboarding1(
                this,
                appSharedPref.firstOnBoarding,
                R.layout.layout_native_onboarding
            )
        }
    }

    private fun listenLanguageAd() {
        AdsManager.nativeLanguageClickAdLive.removeObservers(this)
        AdsManager.nativeLanguageAdLive.observe(this) { ad ->
            if (ad != null) showNativeLanguage(ad) else mBinding.flAds.goneView()
        }
    }

    private fun listenLanguageClickAd() {
        AdsManager.nativeLanguageAdLive.removeObservers(this)
        AdsManager.nativeLanguageClickAdLive.observe(this) { ad ->
            if (ad != null) showNativeLanguage(ad) else mBinding.flAds.goneView()
        }
    }

    private fun showNativeLanguage(ad: ApNativeAd) {
        if (!isNetwork()) {
            mBinding.flAds.goneView()
            return
        }
        mBinding.flAds.visibleView()
        populateNativeAdView(
            this,
            ad,
            mBinding.flAds,
            mBinding.shimmerAds.shimmerNativeSmall
        )
    }

    private fun delayShowDoneButton() {
        if (mBinding.tvDone.visibility == android.view.View.VISIBLE) return

        if (shouldDelayDoneButton) {
            Handler(Looper.getMainLooper()).postDelayed({
                mBinding.ivDone.visibleView()
                mBinding.tvDone.visibleView()
            }, timeDelayDoneButton)
        } else {
            mBinding.ivDone.visibleView()
            mBinding.tvDone.visibleView()
        }
    }

    private fun resubmitLanguageData() {
        languageAdapter.submitList(LanguageData.languages)
    }

    private fun startNextActivity() {
        if (fromSetting) {
            Routes.startMainActivity(this)
        } else {
            appSharedPref.firstLanguage = false
            Routes.startOnBoardingActivity(this)
        }
        finish()
    }
}
