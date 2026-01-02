package com.jagafakta.jagafakta.ui.apk.result

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.history.HistoryPredict
import com.jagafakta.jagafakta.ui.apk.home.MainActivity
import com.jagafakta.jagafakta.ui.apk.profile.ProfileActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var rvRelated: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private val relatedAdapter = RelatedAdapter()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)


        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }


        bottomNav = findViewById(R.id.bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, b)
            insets
        }


        val tvExtractedText = findViewById<TextView>(R.id.tvExtractedText).apply {
            isVerticalScrollBarEnabled = true
            setSingleLine(false)
            movementMethod = ScrollingMovementMethod()
        }


        val rawInput    = intent.getStringExtra("rawInput") ?: ""
        val labelStr    = intent.getStringExtra("label")?.trim() ?: "Valid"
        val relatedList = intent.getParcelableArrayListExtra<RelatedNews>("related") ?: arrayListOf()

        tvExtractedText.text = rawInput


        findViewById<TextView>(R.id.LabelResult).apply {
            text = labelStr.uppercase()
            setTextColor(Color.WHITE)
            setBackgroundResource(
                if (labelStr.equals("Hoax", true)) R.drawable.red else R.drawable.green
            )
        }

        // RecyclerView
        rvRelated = findViewById(R.id.rvRelated)
        rvRelated.layoutManager = LinearLayoutManager(this)
        rvRelated.adapter = relatedAdapter
        Log.d("ResultActivity", "related.size=${relatedList.size}")
        relatedAdapter.submitList(relatedList)

        setupBottomNav()
    }

    private fun setupBottomNav() {

        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {

                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(
                        Intent(this, HistoryPredict::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )

                    true
                }
                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemReselectedListener { /* no-op */ }
    }
}
