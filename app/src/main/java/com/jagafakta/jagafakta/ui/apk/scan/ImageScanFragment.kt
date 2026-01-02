package com.jagafakta.jagafakta.ui.apk.scan//package com.jagafakta.jagafakta.ui.apk.scan
//
//import android.Manifest
//import android.content.ContentValues
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Bundle
//import android.provider.MediaStore
//import android.view.View
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
//
//class ImageScanFragment : Fragment(R.layout.fragment_imagescan) {
//
//    private lateinit var btnGallery: Button
//    private lateinit var btnCamera: Button
//    private lateinit var btnAnalyze: Button
//    private lateinit var imagePreview: ImageView
//    private lateinit var progress: ProgressBar
//
//    private var photoUri: Uri? = null
//    private var extractedText = ""
//
//    // Gallery picker
//    private val pickMedia =
//        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//            uri?.let { onImageSelected(it) }
//        }
//
//    // Camera permission
//    private val requestCameraPerm =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) launchCamera()
//            else Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
//        }
//
//    // Take picture
//    private val takePicture =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
//            if (ok && photoUri != null) onImageSelected(photoUri!!)
//        }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        btnGallery   = view.findViewById(R.id.btnGallery)
//        btnCamera    = view.findViewById(R.id.btnCamera)
//        btnAnalyze   = view.findViewById(R.id.btnAnalyzeImage)
//        imagePreview = view.findViewById(R.id.imagePreview)
//        progress     = view.findViewById(R.id.progressImage)
//
//        btnAnalyze.isEnabled = false
//
//        btnGallery.setOnClickListener {
//            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        }
//
//        btnCamera.setOnClickListener {
//            when {
//                ContextCompat.checkSelfPermission(
//                    requireContext(), Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED -> launchCamera()
//                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
//                    Toast.makeText(
//                        requireContext(),
//                        "Aplikasi butuh akses kamera untuk mengambil foto",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    requestCameraPerm.launch(Manifest.permission.CAMERA)
//                }
//                else -> requestCameraPerm.launch(Manifest.permission.CAMERA)
//            }
//        }
//
//        btnAnalyze.setOnClickListener {
//            if (extractedText.isBlank()) {
//                Toast.makeText(
//                    requireContext(),
//                    "Teks OCR kosong, silakan pilih gambar lagi",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                callNlpAndRetrieveApi(extractedText)
//            }
//        }
//    }
//
//    private fun launchCamera() {
//        photoUri = requireContext().contentResolver.insert(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            ContentValues().apply { put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") }
//        )
//        takePicture.launch(photoUri)
//    }
//
//    private fun onImageSelected(uri: Uri) {
//        imagePreview.setImageURI(uri)
//        runTextRecognition(uri)
//    }
//
//    private fun runTextRecognition(uri: Uri) {
//        progress.visibility = View.VISIBLE
//        btnAnalyze.isEnabled = false
//
//        val image = InputImage.fromFilePath(requireContext(), uri)
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        recognizer.process(image)
//            .addOnSuccessListener { visionText ->
//                extractedText = visionText.text.trim()
//                progress.visibility = View.GONE
//
//                if (extractedText.isBlank()) {
//                    Toast.makeText(requireContext(), "Tidak ada teks terdeteksi", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(requireContext(), "Teks siap dianalisis", Toast.LENGTH_SHORT).show()
//                    btnAnalyze.isEnabled = true
//                }
//            }
//            .addOnFailureListener { e ->
//                progress.visibility = View.GONE
//                Toast.makeText(requireContext(), "OCR gagal: ${e.message}", Toast.LENGTH_LONG).show()
//            }
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
