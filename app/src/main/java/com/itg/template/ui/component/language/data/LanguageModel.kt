package com.itg.template.ui.component.language.data

data class LanguageModel(
    val name: String,
    val iso: String,
    val imageResId: Int,
    val selected: Boolean = false
)