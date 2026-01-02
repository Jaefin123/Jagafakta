package com.jagafakta.jagafakta.ui.apk.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.history.HistoryPredict
import com.jagafakta.jagafakta.ui.apk.home.MainActivity
import com.jagafakta.jagafakta.ui.apk.login.LoginActivity

class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnEditSave: Button
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageView
    private lateinit var progress: ProgressBar
    private lateinit var bottomNav: BottomNavigationView

    private var editMode = false
    private var isGoogle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Bind
        etName       = findViewById(R.id.edtFullname)
        etEmail      = findViewById(R.id.edtEmail)
        etPassword   = findViewById(R.id.edtPassword)
        btnEditSave  = findViewById(R.id.btnedit)
        btnLogout    = findViewById(R.id.btnlogout)
        btnBack      = findViewById(R.id.btnBack)
        progress     = findViewById(R.id.progressProfile)
        bottomNav    = findViewById(R.id.bottom_navigation)

        btnBack.setOnClickListener { finish() }
        setupBottomNav()

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, b); insets
        }

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }

        isGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        // Jika Google, email & password tidak bisa diedit
        if (isGoogle) {
            etEmail.isEnabled = false
            etPassword.visibility = View.GONE
        }

        // Muat profil
        setLoading(true)
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name  = doc.getString("name") ?: user.displayName ?: user.email?.substringBefore("@") ?: "User"
                val email = doc.getString("email") ?: user.email.orEmpty()

                etName.setText(name)
                etEmail.setText(email)


                if (!doc.exists()) {
                    db.collection("users").document(user.uid).set(
                        mapOf("name" to name, "email" to email), SetOptions.merge()
                    )
                }
                setLoading(false)
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Gagal ambil data: ${it.message}", Toast.LENGTH_SHORT).show()
            }


        btnEditSave.setOnClickListener {
            if (!editMode) {
                // masuk ke mode edit
                setEditable(true)
                editMode = true
                btnEditSave.text = getString(R.string.simpan) // ubah teks
            } else {

                saveProfile()
            }
        }


        btnLogout.setOnClickListener { doLogout() }


        setEditable(false)
    }

    private fun setEditable(enabled: Boolean) {
        etName.isEnabled     = enabled

        etEmail.isEnabled    = enabled && !isGoogle
        etPassword.isEnabled = enabled && !isGoogle
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: return
        val newName  = etName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val newPass  = etPassword.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Nama wajib diisi", Toast.LENGTH_SHORT).show(); return
        }
        if (!isGoogle && newEmail.isEmpty()) {
            Toast.makeText(this, "Email wajib diisi", Toast.LENGTH_SHORT).show(); return
        }

        setLoading(true)


        val updates = mutableMapOf<String, Any>("name" to newName, "lastUpdated" to FieldValue.serverTimestamp())
        if (!isGoogle) updates["email"] = newEmail

        db.collection("users").document(user.uid)
            .update(updates as Map<String, Any>)
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Gagal update profil: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {

                if (!isGoogle) {
                    user.updateEmail(newEmail)
                        .addOnFailureListener { e ->
                            setLoading(false)
                            val msg = if (e is FirebaseAuthRecentLoginRequiredException)
                                "Silakan login ulang lalu coba lagi"
                            else
                                "Gagal update email: ${e.message}"
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                        .addOnSuccessListener {
                            if (newPass.isNotEmpty()) {
                                user.updatePassword(newPass)
                                    .addOnCompleteListener { pwTask ->
                                        setLoading(false)
                                        if (!pwTask.isSuccessful) {
                                            Toast.makeText(this, "Profil tersimpan, tapi gagal update password", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this, "Profil & password tersimpan", Toast.LENGTH_SHORT).show()
                                        }

                                        finishSaveSuccess(newName)
                                    }
                                return@addOnSuccessListener
                            } else {
                                setLoading(false)
                                Toast.makeText(this, "Profil tersimpan", Toast.LENGTH_SHORT).show()
                                finishSaveSuccess(newName)
                            }
                        }
                } else {

                    setLoading(false)
                    Toast.makeText(this, "Profil tersimpan", Toast.LENGTH_SHORT).show()
                    finishSaveSuccess(newName)
                }
            }
    }

    private fun finishSaveSuccess(newName: String) {

        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .edit { putString("user_name", newName) }


        editMode = false
        btnEditSave.text = getString(R.string.edit)
        setEditable(false)
    }

    private fun doLogout() {

        auth.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()


        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .edit { clear() }


        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnEditSave.isEnabled = !loading
        btnLogout.isEnabled   = !loading

        if (loading) setEditable(false) else if (editMode) setEditable(true)
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)
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
                R.id.nav_profile -> true
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener { /* no-op */ }
    }
}
