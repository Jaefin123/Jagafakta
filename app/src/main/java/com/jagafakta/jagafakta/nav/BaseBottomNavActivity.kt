package com.jagafakta.jagafakta.nav

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.history.HistoryPredict
import com.jagafakta.jagafakta.ui.apk.home.MainActivity
import com.jagafakta.jagafakta.ui.apk.profile.ProfileActivity

abstract class BaseBottomNavActivity : AppCompatActivity() {

    abstract val selectedTabId: Int  // tiap activity override ini

    protected fun setupBottomNav() {
        val bottom = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottom.selectedItemId = selectedTabId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedTabId) return@setOnItemSelectedListener true

            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryPredict::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        // Optional: padding buat gesture bar
        ViewCompat.setOnApplyWindowInsetsListener(bottom) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, b)
            insets
        }
    }
}
