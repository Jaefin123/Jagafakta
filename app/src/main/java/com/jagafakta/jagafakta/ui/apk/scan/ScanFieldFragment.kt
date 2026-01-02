package com.jagafakta.jagafakta.ui.apk.scan

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.jagafakta.jagafakta.R
import java.io.File

class ScanFieldFragment : Fragment(R.layout.fragment_scan_field) {

    private val vm: ScanViewModel by activityViewModels()

    private lateinit var etInput: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnAdd: ImageView
    private lateinit var btnCamera: ImageView
    private lateinit var btnSearch: Button
    private lateinit var loadingOverlay: View

    private var cameraTempUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { vm.setImageUri(it) }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok && cameraTempUri != null) vm.setImageUri(cameraTempUri!!)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etInput = view.findViewById(R.id.etInput)
        ivPreview = view.findViewById(R.id.ivPreview)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnCamera = view.findViewById(R.id.btnCamera)
        btnSearch = view.findViewById(R.id.btnSearch)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)


        etInput.requestFocus()
        etInput.post { etInput.setSelection(etInput.text.length) }


        etInput.doOnTextChanged { text, _, _, _ ->
            val s = text?.toString().orEmpty()
            if (s != vm.text.value) vm.setText(s)
        }


        vm.text.observe(viewLifecycleOwner) { txt ->
            if (etInput.text.toString() != txt) {
                etInput.setText(txt)
                etInput.setSelection(txt.length.coerceAtLeast(0))
            }
        }

        vm.imageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                ivPreview.visibility = View.VISIBLE
                etInput.visibility = View.GONE
                Glide.with(this).load(uri).centerCrop().into(ivPreview)
            } else {
                ivPreview.visibility = View.GONE
                etInput.visibility = View.VISIBLE
            }
        }

        vm.loading.observe(viewLifecycleOwner) { loading ->
            loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
            btnSearch.isEnabled = !loading
            btnAdd.isEnabled = !loading
            btnCamera.isEnabled = !loading
        }


        btnAdd.setOnClickListener { pickImage.launch("image/*") }
        btnCamera.setOnClickListener {
            cameraTempUri = createImageUriForCamera()
            takePicture.launch(cameraTempUri)
        }
        btnSearch.setOnClickListener { vm.analyzeCurrentText() }

        ivPreview.setOnLongClickListener {
            vm.clearImage()
            true
        }
    }

    override fun onResume() {
        super.onResume()


        if (!vm.text.value.isNullOrBlank() || vm.imageUri.value != null) {
            vm.resetScan()
            etInput.setText("")
            etInput.clearFocus()
            ivPreview.setImageDrawable(null)
            ivPreview.visibility = View.GONE
            etInput.visibility = View.VISIBLE
            etInput.requestFocus()
            etInput.post { etInput.setSelection(0) }
        }

        cameraTempUri?.let { uri ->
            runCatching {
                if ("file" == uri.scheme) File(uri.path!!).delete()
            }
        }
        cameraTempUri = null
    }

    private fun createImageUriForCamera(): Uri {
        val file = File(requireContext().cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }
}
