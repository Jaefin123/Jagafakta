package com.jagafakta.jagafakta.model

import com.google.gson.annotations.SerializedName

data class TextPredictRequest(
    val text: String
)

data class PredictResponse(
    val label: String,

    @SerializedName("bert_prob")
    val bertProb: Probabilities = Probabilities(),

    val retrieval: Double = 0.0,
    val combined: Double = 0.0,

    val articles: List<ArticlePrediction> = emptyList() // default aman
)

data class Probabilities(
    val valid: Double = 0.0,
    val hoax: Double = 0.0
)

data class ArticlePrediction(
    val title: String? = null,
    val description: String? = null,
    val source: String? = null,
    val url: String? = null,
    val urlToImage: String? = null
)
