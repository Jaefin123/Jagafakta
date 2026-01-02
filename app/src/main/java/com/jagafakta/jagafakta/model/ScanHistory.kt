package com.jagafakta.jagafakta.model

class ScanHistory {

    // Request untuk menyimpan/mengirim history scan
    data class ScanHistoryRequest(
        val userId: String,
        val scanType: String,    // "text" atau "image"
        val rawContent: String,  // teks asli atau hasil OCR
        val result: String,      // "FAKTA" atau "HOAX"
        val timestamp: String    // ISO 8601, misal "2025-06-12T10:00:00Z"
    )

    // Item hasil history yang diterima dari server
    data class ScanHistoryItem(
        val historyId: String,
        val scanType: String,
        val rawContent: String,
        val result: String,
        val timestamp: String
    )

    data class BaseResponse(
        val success: Boolean,
        val message: String
    )

}