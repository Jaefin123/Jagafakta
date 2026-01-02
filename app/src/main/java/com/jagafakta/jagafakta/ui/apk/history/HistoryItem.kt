package com.jagafakta.jagafakta.ui.apk.history

import com.jagafakta.jagafakta.ui.apk.result.RelatedNews


data class HistoryItem(
    val id: String,
    val date: String,
    val snippet: String,
    val label: String,
    val timestamp: Long?,
    val related: List<RelatedNews>
)




