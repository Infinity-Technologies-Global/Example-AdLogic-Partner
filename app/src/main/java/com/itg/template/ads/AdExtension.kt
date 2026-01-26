package com.itg.template.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.ads.nkh.admob.Admob
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAdView
import com.itg.template.R
import com.itg.template.ui.bases.ext.dpToPx

fun NkhAd.populateNativeAdView(
    context: Context,
    apNativeAd: ApNativeAd,
    adPlaceHolder: FrameLayout,
    containerShimmerLoading: ShimmerFrameLayout,
    ctaHeightInDp: Int
) {

    if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
        containerShimmerLoading.visibility = View.GONE
        return
    }

    val adView: NativeAdView =
        LayoutInflater.from(context).inflate(apNativeAd.layoutCustomNative, null) as NativeAdView

    containerShimmerLoading.stopShimmer()
    containerShimmerLoading.visibility = View.GONE
    adPlaceHolder.visibility = View.VISIBLE

    val ctaButton = adView.findViewById<View>(R.id.ad_call_to_action)
    ctaButton.updateLayoutParams {
        height = ctaHeightInDp.dpToPx(adPlaceHolder.context).toInt()
    }

    Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
    adPlaceHolder.removeAllViews()
    adPlaceHolder.addView(adView)
}