package com.jagafakta.jagafakta.ui.apk.history

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.ImageView
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.home.MainActivity
import com.jagafakta.jagafakta.ui.apk.profile.ProfileActivity
import com.jagafakta.jagafakta.ui.apk.result.RelatedNews
import com.jagafakta.jagafakta.ui.apk.result.ResultActivity
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryPredict : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sv: SearchView


    private val allHistory = mutableListOf<HistoryItem>()

    private val visibleHistory = mutableListOf<HistoryItem>()

    private lateinit var adapter: HistoryAdapter

    private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())


    private var lastNavClick = 0L


    private val uiHandler = Handler(Looper.getMainLooper())
    private var pendingFilter: Runnable? = null
    private var lastQuery: String = ""

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // RecyclerView
        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.setHasFixedSize(true)

        adapter = HistoryAdapter(
            items = visibleHistory,
            onView = { item ->
                startActivity(
                    Intent(this, ResultActivity::class.java).apply {
                        putExtra("rawInput", item.snippet)
                        putExtra("label", item.label)
                        putParcelableArrayListExtra("related", ArrayList(item.related))
                    }
                )
            },
            onDelete = { item ->
                FirebaseFirestore.getInstance()
                    .collection("history")
                    .document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        allHistory.remove(item)
                        visibleHistory.remove(item)
                        adapter.notifyDataSetChanged()
                    }
            }
        )
        rvHistory.adapter = adapter

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_history
        bottomNav.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.nav_home -> navigateOnce {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    overridePendingTransition(0, 0)
                }
                R.id.nav_history -> true
                R.id.nav_profile -> navigateOnce {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    overridePendingTransition(0, 0)
                }
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener { /* no-op */ }

        // SearchView
        sv = findViewById(R.id.searchView)

        sv.isIconified = false
        sv.queryHint = getString(R.string.search_hint)

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                sv.clearFocus()
                scheduleFilter(query.orEmpty(), immediate = true)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                scheduleFilter(newText.orEmpty())
                return true
            }
        })

        loadHistoryFromFirestore()
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) bottomNav.selectedItemId = R.id.nav_history
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun loadHistoryFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("history")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                allHistory.clear()
                visibleHistory.clear()

                for (doc in snapshot.documents) {
                    val articlesList = (doc.get("articles") as? List<Map<String, Any>>)
                        ?.map { m ->
                            RelatedNews(
                                title      = (m["title"] ?: "") as String,
                                sourceName = (m["sourceName"] ?: "") as String,
                                urlToImage = (m["urlToImage"] ?: "") as String,
                                url        = (m["url"] ?: "") as String
                            )
                        } ?: emptyList()

                    val rawText  = doc.getString("rawInput") ?: ""
                    val labelStr = doc.getString("predictedLabel") ?: "Valid"
                    val ts       = doc.getTimestamp("timestamp")?.toDate()
                    val dateStr  = ts?.let { fmt.format(it) } ?: ""
                    val snippet  = if (rawText.length > 80) rawText.take(80) + "…" else rawText

                    allHistory += HistoryItem(
                        id        = doc.id,
                        date      = dateStr,
                        snippet   = snippet,
                        label     = labelStr,
                        timestamp = ts?.time,
                        related   = articlesList
                    )
                }


                visibleHistory.addAll(allHistory)
                adapter.notifyDataSetChanged()


                if (lastQuery.isNotEmpty()) applyFilter(lastQuery)
            }
    }


    private fun scheduleFilter(q: String, immediate: Boolean = false) {
        lastQuery = q
        pendingFilter?.let { uiHandler.removeCallbacks(it) }
        val task = Runnable { applyFilter(q) }
        pendingFilter = task
        uiHandler.postDelayed(task, if (immediate) 0 else 200)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilter(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        visibleHistory.clear()

        if (q.isEmpty()) {
            visibleHistory.addAll(allHistory)
        } else {

            for (item in allHistory) {
                val hit = item.snippet.lowercase().contains(q) ||
                        item.label.lowercase().contains(q)   ||
                        item.date.lowercase().contains(q)
                if (hit) visibleHistory.add(item)
            }
        }
        adapter.notifyDataSetChanged()
    }


    private fun navigateOnce(block: () -> Unit): Boolean {
        val now = SystemClock.elapsedRealtime()
        return if (now - lastNavClick > 400) {
            lastNavClick = now
            block()
            true
        } else false
    }
}
