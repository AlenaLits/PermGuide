package com.example.permguide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.permguide.R
import com.example.permguide.utils.SettingsManager

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = SettingsManager(requireContext())

        val switchNotifications = view.findViewById<Switch>(R.id.switchNotifications)
        val switchOffline = view.findViewById<Switch>(R.id.switchOffline)
        val switchTheme = view.findViewById<SwitchCompat>(R.id.switchTheme)
        val radiusInput = view.findViewById<EditText>(R.id.editRadius)

        // 📥 Загрузка сохранённых значений
        switchNotifications.isChecked = settings.notificationsEnabled
        switchOffline.isChecked = settings.offlineMode
        switchTheme.isChecked = settings.darkTheme
        radiusInput.setText(settings.radius.toString())

        // 💾 Сохранение уведомлений
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            settings.notificationsEnabled = isChecked
        }

        // 💾 Сохранение офлайн-режима
        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            settings.offlineMode = isChecked
            Toast.makeText(
                requireContext(),
                if (isChecked) "Офлайн-режим включен" else "Офлайн-режим выключен",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 🌙 Смена темы
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            settings.darkTheme = isChecked

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 💾 Сохранение радиуса при потере фокуса
        radiusInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = radiusInput.text.toString().toIntOrNull()
                if (value != null) {
                    settings.radius = value
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Введите корректное число",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}