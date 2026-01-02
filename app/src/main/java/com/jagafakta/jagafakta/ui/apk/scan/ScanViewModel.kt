package com.jagafakta.jagafakta.ui.apk.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jagafakta.jagafakta.model.TextPredictRequest
import com.jagafakta.jagafakta.network.ApiConfig
import com.jagafakta.jagafakta.ui.apk.result.RelatedNews
import com.jagafakta.jagafakta.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val ALLOWED_ROOTS = setOf(
        "kompas.com","detik.com","cnnindonesia.com","tribunnews.com",
        "tvonenews.com","liputan6.com","tempo.co","kumparan.com"
    )

    private fun hostOf(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try { Uri.parse(url).host.orEmpty().lowercase() } catch (_: Exception) { "" }
    }

    private fun matchAllowedRoot(host: String): String? {
        if (host.isBlank()) return null
        return ALLOWED_ROOTS.firstOrNull { root -> host == root || host.endsWith(".$root") }
    }

    private val _text = MutableLiveData("")
    val text: LiveData<String> = _text

    private val _imageUri = MutableLiveData<Uri?>(null)
    val imageUri: LiveData<Uri?> = _imageUri

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _resultEvent = MutableLiveData<Event<ResultPayload>?>()
    val resultEvent: LiveData<Event<ResultPayload>?> = _resultEvent

    fun setText(value: String) { _text.value = value }

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
        runOcr(uri)
    }

    fun clearImage() { _imageUri.value = null }


    fun resetScan() {
        _text.postValue("")
        _imageUri.postValue(null)
        _loading.postValue(false)
        _resultEvent.postValue(null)
    }


    private fun runOcr(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val ctx = getApplication<Application>()
                val inputImage = InputImage.fromFilePath(ctx, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val visionText = recognizer.process(inputImage).await()
                recognizer.close()
                _text.postValue(visionText.text.trim())
            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "OCR error: ${e.message}", e)
                _text.postValue(_text.value ?: "")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun analyzeCurrentText() {
        val txt = _text.value?.trim().orEmpty()
        if (txt.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val predictResp = ApiConfig.apiService.predict(TextPredictRequest(txt))
                val labelStr = if (predictResp.label.equals("HOAX", true)) "Hoax" else "Valid"

                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val historyRef = FirebaseFirestore.getInstance()
                    .collection("history")
                    .add(
                        mapOf(
                            "userId" to uid,
                            "rawInput" to txt,
                            "predictedLabel" to labelStr,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    ).await()

                val relatedList: List<RelatedNews> = (predictResp.articles ?: emptyList())
                    .asSequence()
                    .mapNotNull { a ->
                        val h = hostOf(a.url)
                        val root = matchAllowedRoot(h) ?: return@mapNotNull null
                        root to RelatedNews(
                            title = a.title ?: "",
                            sourceName = root,
                            urlToImage = a.urlToImage ?: "",
                            url = a.url ?: ""
                        )
                    }
                    .distinctBy { it.first }
                    .map { it.second }
                    .take(5)
                    .toList()

                if (relatedList.isNotEmpty()) {
                    val articlesData = relatedList.map { rn ->
                        mapOf(
                            "title" to rn.title,
                            "sourceName" to rn.sourceName,
                            "url" to rn.url,
                            "urlToImage" to rn.urlToImage
                        )
                    }
                    FirebaseFirestore.getInstance()
                        .collection("history")
                        .document(historyRef.id)
                        .update("articles", articlesData)
                        .await()
                }

                _resultEvent.postValue(
                    Event(ResultPayload(txt, labelStr, ArrayList(relatedList)))
                )
            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "analyzeCurrentText error: ${e.message}", e)
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun clearResultEvent() {
        _resultEvent.postValue(null)
    }

    data class ResultPayload(
        val rawInput: String,
        val label: String,
        val related: ArrayList<RelatedNews>
    )
}
