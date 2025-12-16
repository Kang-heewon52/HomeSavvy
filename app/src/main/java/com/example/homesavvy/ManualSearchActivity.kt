package com.example.homesavvy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.appbar.MaterialToolbar

class ManualSearchActivity : AppCompatActivity() {

    private lateinit var viewModel: ManualSearchViewModel
    private val ocrHelper = OcrHelper()

    private var extractedManualText: String = ""

    private lateinit var uploadButton: Button
    private lateinit var searchButton: Button
    private lateinit var queryEditText: EditText
    private lateinit var resultTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var progressLoader: ProgressBar

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    performOcr(bitmap)
                } catch (e: Exception) {
                    statusTextView.text = "오류: 이미지를 불러올 수 없습니다."
                    Toast.makeText(this, "이미지 처리 중 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            statusTextView.text = "상태: 매뉴얼 선택 취소됨."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_search)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_manual_search)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        uploadButton = findViewById(R.id.btn_upload_manual)
        searchButton = findViewById(R.id.btn_smart_search)
        queryEditText = findViewById(R.id.et_user_query)
        resultTextView = findViewById(R.id.tv_search_result)
        statusTextView = findViewById(R.id.tv_ocr_status)
        progressLoader = findViewById(R.id.progress_loader)

        viewModel = ViewModelProvider(this).get(ManualSearchViewModel::class.java)


        lifecycleScope.launch {
            viewModel.searchResult.collect { result ->
                resultTextView.text = result
                updateUiOnSearchState(result)
            }
        }

        uploadButton.setOnClickListener {
            selectImageFromGallery()
        }

        searchButton.setOnClickListener {
            val userQuery = queryEditText.text.toString().trim()

            if (extractedManualText.isBlank()) {
                Toast.makeText(this, "먼저 매뉴얼 사진을 등록하고 텍스트를 추출해야 합니다.", Toast.LENGTH_LONG).show()
            } else if (userQuery.isBlank()) {
                Toast.makeText(this, "질문 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.performSmartSearch(extractedManualText, userQuery)
            }
        }

        updateSearchButtonState(false)
    }

    private fun updateUiOnSearchState(result: String) {
        val isSearching = result == "답변 검색 중..."

        progressLoader.visibility = if (isSearching) View.VISIBLE else View.GONE

        uploadButton.isEnabled = !isSearching
        queryEditText.isEnabled = !isSearching

        if (!isSearching) {
            updateSearchButtonState(extractedManualText.isNotBlank())
        } else {
            searchButton.isEnabled = false
        }

        if (isSearching) {
            statusTextView.text = "상태: Gemini 답변을 분석하는 중..."
        } else if (result.startsWith("오류 발생")) {
            statusTextView.text = "상태: 검색 오류 발생"
        } else if (extractedManualText.isNotBlank()) {
            statusTextView.text = "상태: 답변 완료. 새로운 질문을 해주세요."
        }
    }

    private fun updateSearchButtonState(isTextAvailable: Boolean) {
        searchButton.isEnabled = isTextAvailable
        if (!isTextAvailable) {
            searchButton.text = "매뉴얼을 등록해주세요"
        } else {
            searchButton.text = "스마트 검색 시작 (Gemini 실행)"
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun performOcr(bitmap: Bitmap) {
        statusTextView.text = "상태: OCR 텍스트 추출 중..."
        Toast.makeText(this, "OCR 텍스트 추출 중...", Toast.LENGTH_SHORT).show()

        uploadButton.isEnabled = false
        searchButton.isEnabled = false
        progressLoader.visibility = View.VISIBLE

        ocrHelper.recognizeTextFromImage(
            bitmap = bitmap,
            onSuccess = { text ->
                extractedManualText = text

                updateSearchButtonState(true)
                uploadButton.isEnabled = true
                progressLoader.visibility = View.GONE

                statusTextView.text = "상태: 텍스트 추출 완료! (총 ${text.length} 문자). 이제 질문하세요."
                Toast.makeText(this, "텍스트 추출 완료! 이제 질문하세요.", Toast.LENGTH_LONG).show()

                bitmap.recycle()
            },
            onFailure = { e ->
                extractedManualText = ""
                updateSearchButtonState(false)
                uploadButton.isEnabled = true
                progressLoader.visibility = View.GONE

                statusTextView.text = "상태: OCR 실패 (${e.localizedMessage})"
                Toast.makeText(this, "OCR 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                bitmap.recycle()
            }
        )
    }
}