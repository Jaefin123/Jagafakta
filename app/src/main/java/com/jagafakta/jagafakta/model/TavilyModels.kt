package com.jagafakta.jagafakta.model

// body request
data class TavilySearchRequest(
    val query: String,
    val page_size: Int? = 5
)

// response dari /search
data class TavilySearchResponse(
    val query: String,
    val answer: String?,
    val results: List<TavilyResult>
)

data class TavilyResult(
    val title: String,
    val url: String,
    val content: String?,
    val favicon: String?
)
