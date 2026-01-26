package com.itg.template.ui.component.language

import android.os.Handler
import android.os.Looper
import com.ads.nkh.ads.NkhAd
import com.itg.template.R
import com.itg.template.ads.AdsManager
import com.itg.template.ads.PreLoadNativeListener
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
class LanguageActivity : BaseActivity<ActivityLanguageBinding>(), PreLoadNativeListener {
    private var populateNativeLanguage = false
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
                showNativeLanguageClick()
                delayShowDoneButton()
                selectedIso = it.iso
                LanguageData.selectLanguage(it.iso)
                resubmitLanguageData()
            }
        )
    }


    override fun initViews() {
        isFromSetting = intent.getBooleanExtra(EXTRA_FROM_SETTING, false)
        AdsManager.setPreLoadNativeCallback(this)
        shouldDelayDoneButton = RemoteConfigUtils.shouldDelayLanguageDoneButton()
        timeDelayDoneButton = RemoteConfigUtils.getTimeDelayButtonDoneLanguage()
        initAdapter()
        initLayout()
        initAds()
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
            AdsManager.loadNativeOnboarding(
                this,
                appSharedPref.firstOnBoarding,
                R.layout.layout_native_onboarding
            )
            showNativeLanguage()
        }
    }

    private fun delayShowDoneButton() {
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


    private fun showNativeLanguage() {
        if (populateNativeLanguage) return

        val ad = AdsManager.nativeLanguageAd
        if (ad == null || !isNetwork()) {
            mBinding.flAds.goneView()
            return
        }

        mBinding.flAds.visibleView()
        populateNativeLanguage = true
        NkhAd.getInstance().populateNativeAdView(
            this,
            ad,
            mBinding.flAds,
            mBinding.shimmerAds.shimmerNativeSmall
        )
    }

    private var populateNativeLanguageClick = false
    private fun showNativeLanguageClick() {
        if (populateNativeLanguageClick) return

        val ad = AdsManager.nativeLanguageClickAd
        if (ad == null || !isNetwork()) {
            return
        }

        mBinding.flAds.visibleView()
        populateNativeLanguageClick = true
        NkhAd.getInstance().populateNativeAdView(
            this,
            ad,
            mBinding.flAds,
            mBinding.shimmerAds.shimmerNativeSmall
        )
    }

    override fun onLoadNativeSuccess() {
        if (!populateNativeLanguage && !populateNativeLanguageClick) {
            showNativeLanguage()
        }
    }

    override fun onLoadNativeFail() {
        if (!populateNativeLanguage && !populateNativeLanguageClick) {
            mBinding.flAds.goneView()
        }
    }
}