package com.example.homesavvy

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.example.homesavvy.BuildConfig

class ResultActivity : AppCompatActivity() {

    private lateinit var diagnosisImage: ImageView
    private lateinit var diagnosisResult: TextView
    private lateinit var generativeModel: GenerativeModel
    private lateinit var btnRecommendParts: Button
    private lateinit var recommendationResult: TextView
    private var aiDiagnosisText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        diagnosisImage = findViewById(R.id.iv_diagnosis_image)
        diagnosisResult = findViewById(R.id.tv_diagnosis_result)
        btnRecommendParts = findViewById(R.id.btn_recommend_parts)
        recommendationResult = findViewById(R.id.tv_recommendation_result)

        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            Toast.makeText(this, "🚨 오류: API 키가 로드되지 않았습니다. local.properties를 확인하세요.", Toast.LENGTH_LONG).show()
            Log.e("API_KEY_CHECK", "GEMINI_API_KEY is blank or missing! Check local.properties and build.gradle.")
            btnRecommendParts.isEnabled = false
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_result)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnRecommendParts.setOnClickListener {
            if (aiDiagnosisText.isNotEmpty()) {
                startPartsRecommendation(aiDiagnosisText)
            } else {
                Toast.makeText(this, "먼저 이미지 진단 결과가 나와야 합니다.", Toast.LENGTH_SHORT).show()
            }
        }
        handleIntentData()
    }

    private fun handleIntentData() {
        val intent = intent
        var bitmap: Bitmap? = null

        val cameraBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("image_bitmap", Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Bitmap>("image_bitmap")
        }

        val galleryUri: Uri? = intent.data

        val uriStringFromExtra = intent.getStringExtra("image_uri")
        val imageUriFromExtra: Uri? = if (!uriStringFromExtra.isNullOrEmpty()) Uri.parse(uriStringFromExtra) else null

        if (cameraBitmap != null) {
            bitmap = cameraBitmap
        } else if (galleryUri != null) {
            bitmap = getUriBitmap(galleryUri)
        } else {
            displayAiResult("오류: 진단할 이미지를 찾을 수 없습니다.")
            return
        }

        if (bitmap != null) {
            diagnosisImage.setImageBitmap(bitmap)
            analyzeImageWithGemini(bitmap)
        } else {
            displayAiResult("오류: 이미지를 읽을 수 없습니다. 다시 시도해 주세요.")
        }
    }

    private fun getUriBitmap(imageUri: Uri): Bitmap? {
        val MAX_SIZE = 1024

        return try {
            val tempInput = contentResolver.openInputStream(imageUri)
            val options = android.graphics.BitmapFactory.Options()

            options.inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeStream(tempInput, null, options)
            tempInput?.close()

            var inSampleSize = 1
            if (options.outHeight > MAX_SIZE || options.outWidth > MAX_SIZE) {
                val heightRatio = (options.outHeight.toFloat() / MAX_SIZE.toFloat()).roundToInt()
                val widthRatio = (options.outWidth.toFloat() / MAX_SIZE.toFloat()).roundToInt()
                inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
            }

            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false

            val finalInput = contentResolver.openInputStream(imageUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(finalInput, null, options)
            finalInput?.close()

            bitmap

        } catch (e: Exception) {
            Log.e("ResultActivity", "Error loading bitmap with sampling from URI: ${e.message}", e)
            null
        }
    }

    private fun analyzeImageWithGemini(imageBitmap: Bitmap) {
        displayAiResult("AI가 이미지를 분석 중입니다. 잠시만 기다려 주세요...")
        btnRecommendParts.visibility = View.GONE
        recommendationResult.visibility = View.GONE

        lifecycleScope.launch {
            val maxRetries = 3
            var currentAttempt = 0
            var success = false

            while (currentAttempt < maxRetries && !success) {
                currentAttempt++

                try {
                    displayAiResult("[${currentAttempt}차 시도] AI가 이미지를 분석 중입니다. 잠시만 기다려 주세요...")

                    val prompt = "이것은 집안의 고장 또는 수리가 필요한 부분을 찍은 사진입니다. 사진을 보고 원인은 무엇인지, 사용자가 당장 취해야 할 조치는 무엇인지 5줄 정도로 핵심만 정리해서 한국어로 설명해 주세요."

                    val content = com.google.ai.client.generativeai.type.content {
                        image(imageBitmap)
                        text(prompt)
                    }
                    val response: GenerateContentResponse = generativeModel.generateContent(content)

                    val resultText = response.candidates.first().content.parts.first().asTextOrNull()
                        ?: "분석 결과가 유효하지 않습니다."

                    aiDiagnosisText = resultText
                    displayAiResult(resultText)

                    btnRecommendParts.visibility = View.VISIBLE
                    success = true

                } catch (e: Exception) {
                    Log.e("ResultActivity", "Gemini API Call Failed (Attempt $currentAttempt): ${e.message}", e)

                    if (currentAttempt < maxRetries) {
                        delay(3000L)
                    } else {
                        Toast.makeText(this@ResultActivity, "AI 이미지 분석에 최종 실패했습니다. 서버 상태 및 API 키 설정을 확인해 주세요.", Toast.LENGTH_LONG).show()
                        displayAiResult("AI 분석 중 오류가 발생했습니다. (오류 유형: 서버 과부하 예상)")
                    }
                }
            }
        }
    }

    private fun startPartsRecommendation(diagnosis: String) {

        diagnosisResult.visibility = View.GONE
        recommendationResult.visibility = View.VISIBLE

        displayRecommendationStatus("🛠️ 진단 결과(${diagnosis.substring(0, minOf(diagnosis.length, 10))}...)를 바탕으로 필요한 부품/공구를 분석 중입니다. 잠시만 기다려 주세요...")
        btnRecommendParts.isEnabled = false

        lifecycleScope.launch {

            val maxRetries = 3
            var currentAttempt = 0
            var success = false

            while (currentAttempt < maxRetries && !success) {
                currentAttempt++

                try {
                    displayRecommendationStatus("🛠️ [${currentAttempt}차 시도] 필요한 부품/공구를 분석 중입니다. 잠시만 기다려 주세요...")

                    val recommendationPrompt = """
                    사용자가 제공한 진단 결과는 다음과 같습니다: "$diagnosis"
                    
                    1. **부품 및 공구 설명:** 문제를 해결하기 위해 **필요한 부품**과 **사용할 공구**를 간략히 설명하세요. 각 부품/공구에 대해 **3~4문장 이내**로, 왜 필요한지와 사용법의 핵심만 설명해 주세요.
                    
                    2. **목록 제시:** 상세 설명 후, 반드시 아래와 같은 형식으로 **목록만** 별도로 제시해야 합니다.
                        * **부품 목록**
                            * [부품 1 이름]
                            * [부품 2 이름]
                        * **공구 목록**
                            * [공구 1 이름]
                            * [공구 2 이름]
                """.trimIndent()

                    val content = content {
                        text(recommendationPrompt)
                    }

                    val response: GenerateContentResponse = generativeModel.generateContent(content)

                    val detailedRecommendationText = response.candidates.first().content.parts.first().asTextOrNull()
                        ?: "추천 결과를 찾을 수 없습니다."

                    val finalResult = "**[부품/공구 스마트 추천 상세 설명]**\n${detailedRecommendationText}\n\n[메인 화면의 'Parts & Tools Finder'를 통해 목록 검색이 가능합니다.]"
                    recommendationResult.text = finalResult

                    saveRecommendationsForSimpleSearch(detailedRecommendationText)
                    success = true

                } catch (e: Exception) {
                    Log.e("ResultActivity", "Parts Recommendation Failed (Attempt $currentAttempt): ${e.message}", e)

                    if (currentAttempt < maxRetries) {
                        delay(2000L)
                    } else {
                        Toast.makeText(this@ResultActivity, "부품 추천 실패: AI 서버 과부하 상태입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                        recommendationResult.text = "부품 추천 중 오류가 발생했습니다. AI 서버 상태를 확인해 주세요."
                    }
                }
            }

            btnRecommendParts.isEnabled = true
        }
    }

    private fun saveRecommendationsForSimpleSearch(detailedText: String) {
        val sharedPrefs = getSharedPreferences("HomeSavvyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        val cleanDetailedText = detailedText
            .replace("##", "")
            .replace("#", "")
            .trim()

        val partsRegex = "부품 목록\\s*[:]?\\s*([\\s\\S]*?)(?=\\n*공구 목록|$)".toRegex(RegexOption.IGNORE_CASE)
        val toolsRegex = "공구 목록\\s*[:]?\\s*([\\s\\S]*?)$".toRegex(RegexOption.IGNORE_CASE)

        val rawParts = partsRegex.find(cleanDetailedText)?.groupValues?.get(1)?.trim() ?: ""
        val rawTools = toolsRegex.find(cleanDetailedText)?.groupValues?.get(1)?.trim() ?: ""

        fun cleanAndJoin(rawText: String): String {
            return rawText.split(Regex("(\\n|\\r\\n)"))
                .map { line ->
                    val listLineMatch = Regex("^\\s*([\\*\\-\\d\\.\u2714\u2022])\\s*(.*)").find(line)

                    if (listLineMatch != null) {
                        var itemText = listLineMatch.groupValues[2].trim()

                        itemText = itemText.replace(Regex("\\(.*\\)|\\*\\*.*?\\*\\*|:\\*\\*"), "").trim()

                        if (itemText.endsWith(',')) itemText = itemText.dropLast(1)

                        return@map itemText
                    } else {
                        return@map ""
                    }
                }
                .filter {
                    it.isNotBlank() &&
                            it != "필요성" &&
                            !(it.length <= 2 && Regex("[*#\\-.]").matches(it.trim()))
                }
                .joinToString(", ")
        }

        val partsList = cleanAndJoin(rawParts)
        val toolsList = cleanAndJoin(rawTools)

        editor.putString("LAST_PARTS_LIST", partsList.trimStart(',').trim())
        editor.putString("LAST_TOOLS_LIST", toolsList.trimStart(',').trim())
        editor.apply()

        Log.d("ResultActivity", "Saved Clean Parts: $partsList")
        Log.d("ResultActivity", "Saved Clean Tools: $toolsList")
    }

    private fun displayAiResult(resultText: String) {
        diagnosisResult.text = resultText
        diagnosisResult.visibility = View.VISIBLE
        recommendationResult.visibility = View.GONE
    }

    private fun displayRecommendationStatus(statusText: String) {
        recommendationResult.text = statusText
    }
}