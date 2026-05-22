package com.itg.template.ads

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.ads.module.admob.Admob
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAdView
import com.itg.template.R
import com.itg.template.ui.bases.ext.dpToPx

/**
 * Top-level helper – CTA height/color resolved automatically from [AdsManager.getAdConfig].
 */
fun populateNativeAdView(
    activity: Activity,
    apNativeAd: ApNativeAd,
    adPlaceHolder: FrameLayout,
    containerShimmerLoading: ShimmerFrameLayout,
) {
    if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
        containerShimmerLoading.visibility = View.GONE
        return
    }

    val config = AdsManager.getAdConfig(apNativeAd)

    val adView = LayoutInflater.from(activity)
        .inflate(apNativeAd.layoutCustomNative, null) as NativeAdView

    containerShimmerLoading.stopShimmer()
    containerShimmerLoading.visibility = View.GONE
    adPlaceHolder.visibility = View.VISIBLE

    adView.findViewById<View>(R.id.ad_call_to_action)?.let { ctaButton ->
        ctaButton.updateLayoutParams {
            height = (config?.heightCTA ?: 40).dpToPx(activity).toInt()
        }
        applyCtaColor(ctaButton, config?.colorCTA ?: "default")
    }

    Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
    adPlaceHolder.removeAllViews()
    adPlaceHolder.addView(adView)
}

private fun applyCtaColor(ctaButton: View, colorCTA: String) {
    if (colorCTA == "default" || colorCTA.isBlank()) return
    try {
        val color = Color.parseColor(colorCTA)
        ctaButton.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 20.dpToPx(ctaButton.context).toFloat()
        }
    } catch (_: IllegalArgumentException) { }
}

/**
 * ERainAd extension – explicit ctaHeightInDp, kept for backward-compat.
 */
fun ERainAd.populateNativeAdView(
    activity: Activity,
    apNativeAd: ApNativeAd,
    adPlaceHolder: FrameLayout,
    containerShimmerLoading: ShimmerFrameLayout,
    ctaHeightInDp: Int = 40,
) {
    if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
        containerShimmerLoading.visibility = View.GONE
        return
    }

    val adView: NativeAdView =
        LayoutInflater.from(activity).inflate(apNativeAd.layoutCustomNative, null) as NativeAdView

    containerShimmerLoading.stopShimmer()
    containerShimmerLoading.visibility = View.GONE
    adPlaceHolder.visibility = View.VISIBLE

    adView.findViewById<View>(R.id.ad_call_to_action)?.updateLayoutParams {
        height = ctaHeightInDp.dpToPx(adPlaceHolder.context).toInt()
    }

    Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
    adPlaceHolder.removeAllViews()
    adPlaceHolder.addView(adView)
}
