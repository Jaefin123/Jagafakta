package com.jagafakta.jagafakta.ui.apk.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.history.HistoryPredict
import com.jagafakta.jagafakta.ui.apk.profile.ProfileActivity
import com.jagafakta.jagafakta.ui.apk.result.RelatedNews
import com.jagafakta.jagafakta.ui.apk.result.ResultActivity
import com.jagafakta.jagafakta.ui.apk.scan.ScanFieldFragment
import com.jagafakta.jagafakta.ui.apk.scan.ScanViewModel

class MainActivity : AppCompatActivity() {

    private val vm: ScanViewModel by viewModels()

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvHeader: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var ivLogo: TextView
    private lateinit var ivSubtitle: TextView


    private var lastNavClick = 0L

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHeader   = findViewById(R.id.home)
        tvGreeting = findViewById(R.id.tvgreeting)
        ivLogo     = findViewById(R.id.tvJaga)
        ivSubtitle = findViewById(R.id.tvSlog)
        bottomNav  = findViewById(R.id.bottom_navigation)

        val name = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("user_name", "User") ?: "User"
        tvGreeting.text = "Halo, $name"


        if (savedInstanceState == null) {
            updateTopBar(title = getString(R.string.home), showHero = true)

            supportFragmentManager.findFragmentByTag("home") ?: run {
                loadFragment(ScanFieldFragment(), tag = "home")
            }
        }

        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastNavClick < 350) return@setOnItemSelectedListener true
            lastNavClick = now

            when (item.itemId) {
                R.id.nav_home -> {
                    updateTopBar(getString(R.string.home), showHero = true)

                    val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (current !is ScanFieldFragment) {
                        loadFragment(ScanFieldFragment(), tag = "home")
                    }
                    true
                }
                R.id.nav_history -> {

                    startActivity(
                        Intent(this, HistoryPredict::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener { /* no-op */ }


        vm.resultEvent.observe(this) { event ->
            val payload = event?.getIfNotHandled() ?: return@observe
            showResultPopup(payload.rawInput, payload.label, payload.related)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_home
        }
        updateTopBar(getString(R.string.home), showHero = true)
    }

    private fun updateTopBar(title: String, showHero: Boolean) {
        tvHeader.text = title
        val vis = if (showHero) View.VISIBLE else View.GONE
        tvGreeting.visibility = vis
        ivLogo.visibility = vis
        ivSubtitle.visibility = vis
    }

    private fun loadFragment(fragment: Fragment, tag: String? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    private fun showResultPopup(
        rawInput: String,
        label: String,
        related: ArrayList<RelatedNews>
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_result, null)
        val ivIcon   = dialogView.findViewById<ImageView>(R.id.resultIcon)
        val tvLabel  = dialogView.findViewById<TextView>(R.id.resultLabel)
        val btnDetail= dialogView.findViewById<Button>(R.id.btnDetail)


        ivIcon.setImageDrawable(null)
        ivIcon.background = null
        ivIcon.imageTintList = null
        ivIcon.colorFilter = null
        ivIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE

        val isHoax = label.equals("Hoax", ignoreCase = true)
        ivIcon.setImageResource(if (isHoax) R.drawable.hoax else R.drawable.valid)
        tvLabel.text = if (isHoax) "HOAX" else "VALID"
        tvLabel.setTextColor(if (isHoax) Color.RED else Color.parseColor("#2E7D32"))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.setCanceledOnTouchOutside(true)

        val handler = android.os.Handler(mainLooper)
        val dismissRunnable = Runnable { if (dialog.isShowing) dialog.dismiss() }
        handler.postDelayed(dismissRunnable, 300_000L)
        dialog.setOnDismissListener { handler.removeCallbacks(dismissRunnable) }

        btnDetail.setOnClickListener {
            dialog.dismiss()
            startActivity(
                Intent(this, ResultActivity::class.java).apply {
                    putExtra("rawInput", rawInput)
                    putExtra("label", label)
                    putParcelableArrayListExtra("related", related)
                }
            )
        }

        dialog.show()
    }

}
