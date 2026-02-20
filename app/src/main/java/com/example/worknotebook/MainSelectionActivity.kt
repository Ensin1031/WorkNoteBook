package com.example.worknotebook

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainSelectionActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_selection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_selection_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bottomNavigation = findViewById(R.id.bottom_main_navigation)

        // Установка начального фрагмента
        if (savedInstanceState == null) {
            loadFragment(NotesFragment.newInstance())
            bottomNavigation.selectedItemId = R.id.nav_notes
        }

        // Обработка нажатий на пункты меню
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_notes -> {
                    loadFragment(NotesFragment.newInstance())
                    true
                }
                R.id.nav_meetings -> {
                    loadFragment(MeetingsFragment.newInstance())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(MeetingsFragment.newInstance())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(MeetingsFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}