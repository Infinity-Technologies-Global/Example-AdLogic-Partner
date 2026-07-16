package com.itg.template.ui.component.onboarding

import android.os.Build
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.ads.module.ads.ERainAd
import com.itg.template.R
import com.itg.template.ads.AdRemoteConfig
import com.itg.template.ads.AdsManager
import com.itg.template.ads.RemoteConfigUtils
import com.itg.template.ads.native_confirm_uninstall
import com.itg.template.ads.native_onboarding_fullscreen_1_3
import com.itg.template.databinding.ActivityOnboardingBinding
import com.itg.template.ui.bases.BaseActivity
import com.itg.template.ui.bases.ext.isNetwork
import com.itg.template.ui.component.onboarding.adapter.OnboardingAdapter
import com.itg.template.ui.component.onboarding.model.OnboardingItem
import com.itg.template.ui.component.onboarding.viewmodel.OnboardingViewModel
import com.itg.template.ui.component.uninstall.ShortcutManager
import com.itg.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class OnBoardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    override val shouldShowNavigationBars = RemoteConfigUtils.getOnShowNavigationButton()

    override fun getLayoutActivity(): Int = R.layout.activity_onboarding

    private val onboardingViewModel by viewModels<OnboardingViewModel>()
    private lateinit var onboardingAdapter: OnboardingAdapter
    private val onboardingItems = mutableListOf<OnboardingItem>()

    override fun initViews() {
        initPage()
        initOnboardingItems()
        applyUninstallWidgetShortcutsFromRemoteConfig()

        mBinding.root.postDelayed({
            AdsManager.loadNativeOnboarding4(
                this,
                appSharedPref.firstOnBoarding,
                R.layout.layout_native_onboarding
            )
            AdsManager.loadNativeOnboardingFull(
                this,
                appSharedPref.firstOnBoarding,
                R.layout.layout_native_onboarding_full
            )

            AdsManager.loadInterOnboarding(this)

        }, 100L)
    }

    private fun applyUninstallWidgetShortcutsFromRemoteConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        if (ERainAd.getInstance().getShouldDisplayWidgetUninstall(
                RemoteConfigUtils.getOnEnableUninstallWidget())) {
            ShortcutManager.initShortCut(this@OnBoardingActivity)
        }
    }

    override fun observerData() {
        super.observerData()
        onboardingViewModel.isNeedNextPage.observe(this) {
            val currentPosition = mBinding.viewPager.currentItem
            if (currentPosition < onboardingAdapter.itemCount - 1) {
                mBinding.viewPager.currentItem = currentPosition + 1
            } else startNextActivity()
        }
    }

    private fun initPage() {
        onboardingAdapter = OnboardingAdapter(supportFragmentManager, lifecycle)
        mBinding.viewPager.adapter = onboardingAdapter
        mBinding.viewPager.clipToPadding = false
        mBinding.viewPager.clipChildren = false
        mBinding.viewPager.offscreenPageLimit = 4
        mBinding.viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(100))
        compositePageTransformer.addTransformer { view, position ->
            val r = 1 - abs(position)
            view.scaleY = 0.8f + r * 0.2f
            val absPosition = abs(position)
            view.alpha = 1.0f - (1.0f - 0.3f) * absPosition
        }
        mBinding.viewPager.setPageTransformer(compositePageTransformer)

        mBinding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {}
        })
    }

    private fun initOnboardingItems() {
        onboardingItems.clear()
        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_1,
                description = R.string.onboarding_title_1,
                textButton = R.string.next,
                imageResId = R.mipmap.ic_launcher,
                positionIndicator = 0,
                isHasNativeOnPage1 = true
            )
        )
        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_2,
                description = R.string.onboarding_title_2,
                textButton = R.string.next,
                imageResId = R.mipmap.ic_launcher,
                positionIndicator = 1
            )
        )
        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_3,
                description = R.string.onboarding_title_3,
                textButton = R.string.next,
                imageResId = R.mipmap.ic_launcher,
                positionIndicator = 2,
            )
        )

        if (isNetwork(this@OnBoardingActivity) && ERainAd.getInstance().getShouldDisplayNativeOnboardingFull1(
                AdRemoteConfig.native_onboarding_fullscreen_1_3.enableUaCheck))
            onboardingItems.add(
                OnboardingItem(
                    isHasNativeFull = true
                )
            )


        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_4,
                description = R.string.onboarding_title_4,
                textButton = R.string.next,
                imageResId = R.mipmap.ic_launcher,
                positionIndicator = 3,
                isHasNativeOnPage4 = true
            )
        )
        onboardingAdapter.submitData(onboardingItems)
    }

    private fun startNextActivity() {
        appSharedPref.firstOnBoarding = false
        AdsManager.showInterOnboarding(this) {
            Routes.startMainActivity(this)
            finish()
        }
    }

    override fun onBackPressed() {
        // block back press on onboarding
    }
}
