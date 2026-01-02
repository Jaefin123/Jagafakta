package com.jagafakta.jagafakta.ui.apk.register

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // view binding
    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPass: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()


        editName    = findViewById(R.id.editName)
        editEmail   = findViewById(R.id.editEmail)
        editPass    = findViewById(R.id.editPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin     = findViewById(R.id.tvLogin)
        progress    = findViewById(R.id.progressRegister)

        btnRegister.setOnClickListener {
            val name  = editName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val pass  = editPass.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                editPass.error = "Password minimal 6 karakter"
                return@setOnClickListener
            }
            doRegister(name, email, pass)
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    @SuppressLint("UseKtx")
    private fun doRegister(name: String, email: String, pass: String) {
        progress.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                // simpan profil di Firestore
                val profile = mapOf("name" to name, "email" to email)
                db.collection("users").document(uid).set(profile)
                    .addOnSuccessListener {
                        progress.visibility = View.GONE

                        // ** Simpan nama ke SharedPreferences **
                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_name", name)
                            .apply()

                        Toast.makeText(
                            this,
                            "Registrasi berhasil! Silakan login.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progress.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(
                            this,
                            "Gagal simpan profil: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                btnRegister.isEnabled = true
                Toast.makeText(
                    this,
                    "Registrasi gagal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
