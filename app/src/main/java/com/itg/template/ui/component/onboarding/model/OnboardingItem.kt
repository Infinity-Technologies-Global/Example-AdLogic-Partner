package com.itg.template.ui.component.onboarding.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingItem(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val textButton: Int,
    @DrawableRes val imageResId: Int,
    val positionIndicator: Int,
    val isHasNativeOnPage1: Boolean = false,
    val isHasNativeOnPage4: Boolean = false,
    val isHasNativeFull: Boolean = false,
): Parcelable