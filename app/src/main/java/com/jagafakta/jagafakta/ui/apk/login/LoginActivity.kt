@file:Suppress("DEPRECATION")

package com.jagafakta.jagafakta.ui.apk.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.home.MainActivity
import com.jagafakta.jagafakta.ui.apk.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    companion object { private const val RC_GOOGLE = 1001 }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var editEmail: EditText
    private lateinit var editPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogle: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgot: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("id")

        // Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // bind views
        editEmail  = findViewById(R.id.editEmail)
        editPass   = findViewById(R.id.editPassword)
        btnLogin   = findViewById(R.id.btnLogin)
        btnGoogle  = findViewById(R.id.btngoogle)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgot   = findViewById(R.id.tvForgot)          // <-- tambahkan di XML
        progress   = findViewById(R.id.progressLogin)

        btnLogin.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val pass  = editPass.text.toString().trim()
            if (!isValidEmail(email)) {
                editEmail.error = "Email tidak valid"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                editPass.error = "Password minimal 6 karakter"
                return@setOnClickListener
            }
            doEmailLogin(email, pass)
        }

        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE)
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


        tvForgot.setOnClickListener { showForgotDialog() }
    }

    private fun isValidEmail(email: String) =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    @SuppressLint("UseKtx")
    private fun doEmailLogin(email: String, pass: String) {
        setLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user!!
                val name = firebaseUser.displayName
                    ?: firebaseUser.email?.substringBefore('@')
                    ?: "User"
                getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .edit().putString("user_name", name).apply()
                goToMain()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Google Sign-In
    @SuppressLint("UseKtx")
    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val cred    = GoogleAuthProvider.getCredential(account.idToken!!, null)
                setLoading(true)
                auth.signInWithCredential(cred)
                    .addOnSuccessListener { authResult ->

                        val name = authResult.user?.displayName
                            ?: account.displayName
                            ?: account.email?.substringBefore('@')
                            ?: "User"
                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            .edit().putString("user_name", name).apply()
                        goToMain()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Google login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //  Lupa Password
    private fun showForgotDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val etEmail = view.findViewById<EditText>(R.id.etEmailReset)
        etEmail.setText(editEmail.text?.toString()?.trim().orEmpty()) // prefill

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset Kata Sandi")
            .setView(view)
            .setPositiveButton("Kirim", null) // kita override biar bisa validasi dulu
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (!isValidEmail(email)) {
                    etEmail.error = "Email tidak valid"
                    return@setOnClickListener
                }
                setLoading(true)
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        setLoading(false)

                        Toast.makeText(
                            this,
                            "Jika email terdaftar, tautan reset sudah dikirim.",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    }
            }
        }
        dialog.show()
    }

    private fun setLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnGoogle.isEnabled = !show
        tvRegister.isEnabled = !show
        tvForgot.isEnabled = !show
    }

    private fun goToMain() {
        setLoading(false)
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
