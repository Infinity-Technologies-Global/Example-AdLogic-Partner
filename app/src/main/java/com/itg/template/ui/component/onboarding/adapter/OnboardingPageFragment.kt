package com.itg.template.ui.component.onboarding.adapter

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.ads.nkh.ads.NkhAd
import com.bumptech.glide.Glide
import com.itg.template.R
import com.itg.template.ads.AdRemoteConfig
import com.itg.template.ads.AdsManager
import com.itg.template.ads.RemoteConfigUtils
import com.itg.template.ads.native_onboarding_1_1
import com.itg.template.ads.populateNativeAdView
import com.itg.template.databinding.FragmentOnboardingPageBinding
import com.itg.template.ui.bases.BaseFragment
import com.itg.template.ui.bases.ext.click
import com.itg.template.ui.bases.ext.goneView
import com.itg.template.ui.bases.ext.invisibleView
import com.itg.template.ui.bases.ext.parcelable
import com.itg.template.ui.bases.ext.visibleView
import com.itg.template.ui.component.onboarding.model.OnboardingItem
import com.itg.template.ui.component.onboarding.viewmodel.OnboardingViewModel

class OnboardingPageFragment : BaseFragment<FragmentOnboardingPageBinding>() {

    override fun getLayoutFragment(): Int = R.layout.fragment_onboarding_page

    companion object {

        private const val ARG_ONBOARDING_ITEM = "arg_onboarding_item"

        fun newInstance(onboardingItem: OnboardingItem) = OnboardingPageFragment().apply {
            arguments = bundleOf(
                ARG_ONBOARDING_ITEM to onboardingItem
            )
        }
    }

    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()

    private var onboardingItem: OnboardingItem = OnboardingItem(
        title = R.string.onboarding_title_1,
        description = R.string.onboarding_des_1,
        textButton = R.string.next,
        imageResId = R.drawable.ic_vietnamese,
        positionIndicator = 0
    )

    override fun initViews() {
        arguments?.parcelable<OnboardingItem>(ARG_ONBOARDING_ITEM)?.let {
            onboardingItem = it
        }
        updateLayout()
        initAd()
    }

    override fun onResume() {
        super.onResume()
        initAd()
    }

    override fun observerData() {
        onboardingViewModel.nativeAdFullLoaded.observe(viewLifecycleOwner) {
            if (it && onboardingItem.positionIndicator == 2) {
                initAd()
            }
        }
    }

    override fun onClickViews() {
        mBinding.btnNext.click {
            onboardingViewModel.onNextClicked()
        }
        mBinding.imgCloseAdsFull.click {
            onboardingViewModel.onNextClicked()
        }
    }

    private fun updateLayout() {
        Glide.with(this).load(onboardingItem.imageResId).into(mBinding.imgOnboarding)
        mBinding.tvTitle.text = getString(onboardingItem.title)
        mBinding.tvDes.text = getString(onboardingItem.description)
        mBinding.btnNext.text = getString(onboardingItem.textButton)
        mBinding.imgIndicator0.setImageResource(R.drawable.ic_onboarding_indicator)
        mBinding.imgIndicator1.setImageResource(R.drawable.ic_onboarding_indicator)
        mBinding.imgIndicator2.setImageResource(R.drawable.ic_onboarding_indicator)
        mBinding.imgIndicator3.setImageResource(R.drawable.ic_onboarding_indicator)
        when (onboardingItem.positionIndicator) {
            0 -> mBinding.imgIndicator0.setImageResource(R.drawable.ic_onboarding_indicator_selected)
            1 -> mBinding.imgIndicator1.setImageResource(R.drawable.ic_onboarding_indicator_selected)
            2 -> mBinding.imgIndicator2.setImageResource(R.drawable.ic_onboarding_indicator_selected)
            3 -> mBinding.imgIndicator3.setImageResource(R.drawable.ic_onboarding_indicator_selected)
        }
    }

    private fun initAd() {
        if (onboardingItem.isHasNativeFull) {
            val nativeAd = AdsManager.nativeAdOnBoardingFull
            if (nativeAd != null) {
                mBinding.layoutAdsFull.visibleView()
                mBinding.imgCloseAdsFull.visibleView()
                mBinding.layoutContent.invisibleView()
                NkhAd.getInstance().populateNativeAdView(
                    requireActivity(),
                    nativeAd,
                    mBinding.layoutAdsFull,
                    mBinding.shimmerAdsFull.shimmerNativeFull
                )
            } else {
                mBinding.layoutContent.visibleView()
                mBinding.layoutAdsFull.invisibleView()
                mBinding.layoutAds.invisibleView()
                mBinding.imgCloseAdsFull.invisibleView()
            }
        } else if (onboardingItem.isHasNativeOnPage1 || onboardingItem.isHasNativeOnPage4) {
            val nativeAd =
                if (onboardingItem.isHasNativeOnPage1) AdsManager.nativeOnboarding1Ad else AdsManager.nativeOnboarding4Ad
            if (nativeAd != null) {
                mBinding.layoutAds.visibleView()
                NkhAd.getInstance().populateNativeAdView(
                    requireActivity(),
                    nativeAd,
                    mBinding.layoutAds,
                    mBinding.shimmerAds.shimmerNativeMedium,
                    AdRemoteConfig.native_onboarding_1_1.heightCTA
                )
            } else {
                mBinding.layoutAds.invisibleView()
            }
            mBinding.layoutContent.visibleView()
            mBinding.layoutAdsFull.invisibleView()
            mBinding.imgCloseAdsFull.invisibleView()
        } else {
            mBinding.layoutContent.visibleView()
            mBinding.layoutAds.goneView()
            mBinding.layoutAdsFull.invisibleView()
            mBinding.imgCloseAdsFull.invisibleView()
        }
    }
}