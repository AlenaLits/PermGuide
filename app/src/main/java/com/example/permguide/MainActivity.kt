package com.example.permguide

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.permguide.ui.HomeFragment
import com.example.permguide.ui.ListFragment
import com.example.permguide.ui.MapFragment
import com.example.permguide.ui.SettingsFragment
import android.widget.ImageButton
import androidx.core.view.size

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContentView(R.layout.activity_main)
        // Инициализируем кнопку настроек (которую мы добавили в Toolbar)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)

        // Инициализируем нижнюю навигацию
        bottomNavigation = findViewById(R.id.bottomNavigation)

        btnSettings.setOnClickListener {
            openFragment(SettingsFragment())
            bottomNavigation.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNavigation.menu.size) {
                bottomNavigation.menu.getItem(i).isChecked = false
            }
            bottomNavigation.menu.setGroupCheckable(0, true, true)
        }

        setupBottomNavigation()
    }
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> openFragment(HomeFragment())
                R.id.nav_map -> openFragment(MapFragment())
                R.id.nav_list -> openFragment(ListFragment())
            }
            true
        }
        // Установка фрагмента по умолчанию
        if (supportFragmentManager.fragments.isEmpty()) {
            openFragment(HomeFragment())
        }
    }
    private fun openFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}