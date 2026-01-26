package com.itg.template.ui.component.language.data

import android.content.res.Resources
import android.os.Build
import com.itg.template.R

object LanguageData {

    private val _languages = mutableListOf(
        LanguageModel(name = "English", iso = "en", imageResId = R.drawable.ic_english),
        LanguageModel(name = "Hindi", iso = "hi", imageResId = R.drawable.ic_hindi),
        LanguageModel(name = "Arabic", iso = "ar", imageResId = R.drawable.ic_arabic),
        LanguageModel(name = "Spanish", iso = "es", imageResId = R.drawable.ic_spanish),
        LanguageModel(name = "Croatian", iso = "hr", imageResId = R.drawable.ic_croatia),
        LanguageModel(name = "Czech", iso = "cs", imageResId = R.drawable.ic_czech_republic),
        LanguageModel(name = "Dutch", iso = "nl", imageResId = R.drawable.ic_dutch),
        LanguageModel(name = "Filipino", iso = "fil", imageResId = R.drawable.ic_filipino),
        LanguageModel(name = "French", iso = "fr", imageResId = R.drawable.ic_france),
        LanguageModel(name = "German", iso = "de", imageResId = R.drawable.ic_german),
        LanguageModel(name = "Indonesian", iso = "in", imageResId = R.drawable.ic_indonesian),
        LanguageModel(name = "Italian", iso = "it", imageResId = R.drawable.ic_italian),
        LanguageModel(name = "Japanese", iso = "ja", imageResId = R.drawable.ic_japanese),
        LanguageModel(name = "Korean", iso = "ko", imageResId = R.drawable.ic_korean),
        LanguageModel(name = "Malay", iso = "ms", imageResId = R.drawable.ic_malay),
        LanguageModel(name = "Polish", iso = "pl", imageResId = R.drawable.ic_polish),
        LanguageModel(name = "Portuguese", iso = "pt", imageResId = R.drawable.ic_portugal),
        LanguageModel(name = "Russian", iso = "ru", imageResId = R.drawable.ic_russian),
        LanguageModel(name = "Serbian", iso = "sr", imageResId = R.drawable.ic_serbian),
        LanguageModel(name = "Swedish", iso = "sv", imageResId = R.drawable.ic_swedish),
        LanguageModel(name = "Turkish", iso = "tr", imageResId = R.drawable.ic_turkish),
        LanguageModel(name = "Vietnamese", iso = "vi", imageResId = R.drawable.ic_vietnamese),
        LanguageModel(name = "China", iso = "zh", imageResId = R.drawable.ic_china)
    )


    val defaultLanguage: LanguageModel
        get() = _languages.first()

    val systemLanguage: LanguageModel
        get() = run {
            val systemIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0].language
            } else {
                Resources.getSystem().configuration.locale.language
            }
            _languages.find { it.iso == systemIso } ?: defaultLanguage
        }

    val languages: List<LanguageModel>
        get() = run {
            val result = _languages.take(10).toMutableList()
            if (result.contains(systemLanguage).not()) {
                result.add(0, systemLanguage)
            } else {
                result.remove(systemLanguage)
                result.add(0, systemLanguage)
            }
            result
        }


    fun selectLanguage(iso: String) {
        val result = mutableListOf<LanguageModel>()
        _languages.forEach {
            if (it.iso == iso) {
                result.add(it.copy(selected = true))
            } else {
                result.add(it.copy(selected = false))
            }
        }
        _languages.clear()
        _languages.addAll(result)
    }
}