package com.itg.template.ui.component.onboarding.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.itg.template.ui.bases.BaseViewModel
import kotlinx.coroutines.launch

class OnboardingViewModel: BaseViewModel() {

    private val _isNeedNextPage = MutableLiveData<Boolean>()
    val isNeedNextPage: LiveData<Boolean> = _isNeedNextPage

    private val _nativeAdFullLoaded = MutableLiveData<Boolean>()
    val nativeAdFullLoaded: LiveData<Boolean> = _nativeAdFullLoaded

    fun onNextClicked() {
        viewModelScope.launch {
            _isNeedNextPage.value = true
        }
    }

    fun notifyNativeAdFullLoaded() {
        viewModelScope.launch {
            _nativeAdFullLoaded.value = true
        }
    }
}