package com.example.permguide.utils

import android.content.Context

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", false)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    var offlineMode: Boolean
        get() = prefs.getBoolean("offline", false)
        set(value) = prefs.edit().putBoolean("offline", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", false)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var radius: Int
        get() = prefs.getInt("radius", 50)
        set(value) = prefs.edit().putInt("radius", value).apply()
}