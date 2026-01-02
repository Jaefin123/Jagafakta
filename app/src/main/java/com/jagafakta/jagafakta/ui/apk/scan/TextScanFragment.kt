//package com.jagafakta.jagafakta.ui.apk.scan
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.FirebaseFirestore
//import com.jagafakta.jagafakta.Config
//import com.jagafakta.jagafakta.R
//import com.jagafakta.jagafakta.model.TavilySearchRequest
//import com.jagafakta.jagafakta.model.TextPredictRequest
//import com.jagafakta.jagafakta.network.ApiConfig
//import com.jagafakta.jagafakta.ui.apk.result.RelatedNews
//import com.jagafakta.jagafakta.ui.apk.result.ResultActivity
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//
//class TextScanFragment : Fragment(R.layout.fragment_textscan) {
//    private lateinit var editText: EditText
//    private lateinit var btnAnalyze: Button
//    private lateinit var progress: ProgressBar
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        editText   = view.findViewById(R.id.inputnews)
//        btnAnalyze = view.findViewById(R.id.btnAnalyzeText)
//        progress   = view.findViewById(R.id.progressText)
//
//        btnAnalyze.setOnClickListener {
//            val text = editText.text.toString().trim()
//            if (text.isEmpty()) {
//                Toast.makeText(requireContext(), "Teks tidak boleh kosong", Toast.LENGTH_SHORT).show()
//            } else {
//                callNlpAndRetrieveApi(text)
//            }
//        }
//    }
//
//    private fun callNlpAndRetrieveApi(text: String) {
//        // 1) tampilkan loading
//        progress.visibility = View.VISIBLE
//        btnAnalyze.isEnabled = false
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // ——— 2) PREDIKSI FASTAPI ———
//                val predictResp = ApiConfig.apiService.predict(
//                    TextPredictRequest(text)
//                )
//                val labelStr = if (predictResp.label.equals("HOAX", true)) "Hoax" else "Valid"
//
//                // ——— 3) SIMPAN HISTORY KE FIRESTORE ———
//                val uid = FirebaseAuth.getInstance().currentUser!!.uid
//                val historyRef = FirebaseFirestore.getInstance()
//                    .collection("history")
//                    .add(mapOf(
//                        "userId"         to uid,
//                        "rawInput"       to text,
//                        "predictedLabel" to labelStr,
//                        "timestamp"      to FieldValue.serverTimestamp()
//                    ))
//                    .await()
//
//                // ——— 4) PANGGIL TAVILY SEARCH ———
//                val bearer = "Bearer ${Config.TAVILY_API_KEY}"
//                val tavilyReq = TavilySearchRequest(
//                    query     = text.take(100),
//                    page_size = 5
//                )
//                val tavilyResp = ApiConfig.tavilyService.searchTavily(
//                    authorization = bearer,
//                    body          = tavilyReq
//                )
//
//                // ——— 5) MAP KE MODEL UI ———
//                val relatedList = tavilyResp.results.map { r ->
//                    RelatedNews(
//                        title      = r.title,
//                        sourceName = "",              // Tavily tidak kirim sumber
//                        urlToImage = r.favicon ?: "", // fallback favicon
//                        url        = r.url
//                    )
//                }
//
//                // ——— 6) UPDATE FIRESTORE DENGAN ARTIKEL ———
//                val articlesData = relatedList.map { rn ->
//                    mapOf(
//                        "title"      to rn.title,
//                        "sourceName" to rn.sourceName,
//                        "url"        to rn.url,
//                        "urlToImage" to rn.urlToImage
//                    )
//                }
//                FirebaseFirestore.getInstance()
//                    .collection("history")
//                    .document(historyRef.id)
//                    .update("articles", articlesData)
//                    .await()
//
//                // ——— 7) NAVIGASI KE ResultActivity ———
//                withContext(Dispatchers.Main) {
//                    progress.visibility = View.GONE
//                    btnAnalyze.isEnabled = true
//
//                    Intent(requireContext(), ResultActivity::class.java).apply {
//                        putExtra("rawInput", text)
//                        putExtra("label", labelStr)
//                        putParcelableArrayListExtra(
//                            "related",
//                            ArrayList(relatedList)
//                        )
//                    }.also { startActivity(it) }
//                }
//
//            } catch (e: Exception) {
//                // jika ada error apa pun
//                withContext(Dispatchers.Main) {
//                    progress.visibility = View.GONE
//                    btnAnalyze.isEnabled = true
//                    Toast.makeText(
//                        requireContext(),
//                        "Error: ${e.localizedMessage}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }
//}
