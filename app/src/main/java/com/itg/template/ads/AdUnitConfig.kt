package com.itg.template.ads

import androidx.annotation.Keep
@Keep
data class AdUnitConfig(
    val id: String,
    val isEnable: Boolean,
    val reloadIntervalSeconds: Int? = null,
    val colorCTA: String = "default",
    val heightCTA: Int = 40,
    val positionCTA: String = "BOTTOM"
)
