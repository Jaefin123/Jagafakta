package com.jagafakta.jagafakta.model

class AuthModels {

    // Request untuk login
    data class LoginRequest(
        val email: String,
        val password: String
    )

    // Response dari login
    data class LoginResponse(
        val token: String,
        val userId: String,
        val nama: String? = null // tambahkan field lain kalau ada
    )

    // Request untuk register
    data class RegisterRequest(
        val name: String,
        val email: String,
        val password: String
    )

    // Response dari register
    data class RegisterResponse(
        val success: Boolean,
        val message: String
    )
}