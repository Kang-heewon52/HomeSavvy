package com.example.homesavvy

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private val featureList = listOf(

        FeatureItem(
            id = 1,
            title = "Smart Diagnosis",
            description = "고장 사진을 찍으면 AI가 원인을 진단하고 수리 가이드를 제공합니다.",
            iconResId = R.drawable.ic_wrench_fix
        ),
        FeatureItem(
            id = 2,
            title = "Parts & Tools Finder",
            description = "필요한 부품과 공구를 찾고, 주변 매장이나 최저가 구매처를 연결합니다.",
            iconResId = R.drawable.ic_tool_search
        ),
        FeatureItem(
            id = 3,
            title = "Cycle Predictor",
            description = "소모품 교체 주기를 예측하고, 시기가 되면 자동으로 알림을 보냅니다.",
            iconResId = R.drawable.ic_cycle_predict
        ),
        FeatureItem(
            id = 4,
            title = "Manual Smart Search",
            description = "매뉴얼을 업로드하고, 궁금한 점을 질문하면 AI가 즉시 요약해 줍니다.",
            iconResId = R.drawable.ic_manual_search
        )
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val recyclerView: RecyclerView = findViewById(R.id.rv_core_features)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val adapter = FeatureAdapter(featureList) { feature ->
            when (feature.id) {
                1 -> {
                    val intent = Intent(this, DiagnosisActivity::class.java)
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this, RecommendationListActivity::class.java)
                    startActivity(intent)
                }
                3 -> {
                    val intent = Intent(this, CyclePredictionActivity::class.java)
                    startActivity(intent)
                }
                4 -> {
                    val intent = Intent(this, ManualSearchActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                 Toast.makeText(this, "${feature.title} 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        applyAiSummaryHighlight()
    }

    private fun applyAiSummaryHighlight() {
        val aiSummaryTextView: TextView = findViewById(R.id.tv_ai_summary)

        val summaryData = analyzeCycleData(this)
        val fullText = buildSummaryText(summaryData)

        val spannableString = SpannableString(fullText)

        val baseColor = ContextCompat.getColor(this, R.color.color_base_summary_text)
        val generalAccentColor = ContextCompat.getColor(this, R.color.color_accent_general)
        val warningAccentColor = ContextCompat.getColor(this, R.color.color_accent_warning)

        spannableString.setSpan(
            ForegroundColorSpan(baseColor),
            0,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val generalKeywords = summaryData.imminentReplacement.joinToString(", ").ifEmpty { "없음" }
        val replacementMarker = "교체 임박: "

        if (generalKeywords != "없음") {
            val generalStart = fullText.indexOf(replacementMarker) + replacementMarker.length
            val generalEnd = generalStart + generalKeywords.length

            if (generalStart < generalEnd && generalEnd <= fullText.length) {
                spannableString.setSpan(
                    ForegroundColorSpan(generalAccentColor),
                    generalStart,
                    generalEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val warningKeywords = summaryData.urgentCheckNeeded.joinToString(", ").ifEmpty { "없음" }
        val warningMarker = "점검 필요: "

        if (warningKeywords != "없음") {
            val warningStart = fullText.indexOf(warningMarker) + warningMarker.length
            val warningEnd = warningStart + warningKeywords.length

            if (warningStart < warningEnd && warningEnd <= fullText.length) {
                spannableString.setSpan(
                    ForegroundColorSpan(warningAccentColor),
                    warningStart,
                    warningEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        aiSummaryTextView.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    private fun buildSummaryText(data: SummaryData): String {
        val replacementText = data.imminentReplacement.joinToString(", ").ifEmpty { "없음" }
        val checkText = data.urgentCheckNeeded.joinToString(", ").ifEmpty { "없음" }

        return "✅ 이번 주 교체 임박: $replacementText, 점검 필요: $checkText"
    }
}