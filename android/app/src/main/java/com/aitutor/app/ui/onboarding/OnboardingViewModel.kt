package com.aitutor.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.local.OnboardingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    fun onOnboardingComplete() {
        viewModelScope.launch {
            onboardingDataStore.setOnboardingDone()
        }
    }
}
