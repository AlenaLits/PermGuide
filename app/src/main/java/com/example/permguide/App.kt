package com.example.permguide

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.permguide.utils.SettingsManager
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val settings = SettingsManager(this)
        // MainActivity
        MapKitFactory.setApiKey("295f9ed3-bae0-49d1-b7d9-2ee24b1f9b09")
        MapKitFactory.initialize(this)

        if (settings.darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}