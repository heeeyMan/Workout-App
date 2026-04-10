package com.workout.android.ui.settings

import androidx.lifecycle.ViewModel
import com.workout.android.data.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val themePreferences: ThemePreferences) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(themePreferences.isDarkTheme)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(dark: Boolean) {
        themePreferences.isDarkTheme = dark
        _isDarkTheme.value = dark
    }
}
