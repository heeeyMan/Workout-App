package com.workout.android.data

import android.content.Context

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true) // dark by default
        set(value) { prefs.edit().putBoolean("dark_theme", value).apply() }
}
