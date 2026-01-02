
package com.jagafakta.jagafakta.model

data class TavilyResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<ArticleNews>
)

data class ArticleNews(
    val source: Source?,
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val content: String?
)

data class Source(
    val id: String?,
    val name: String?
)

