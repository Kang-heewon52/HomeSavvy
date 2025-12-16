package com.example.homesavvy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesavvy.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManualSearchViewModel : ViewModel() {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    private val _searchResult = MutableStateFlow<String>("질문을 입력해주세요.")
    val searchResult: StateFlow<String> = _searchResult

    fun performSmartSearch(manualText: String, userQuery: String) {
        if (manualText.isBlank() || userQuery.isBlank()) {
            _searchResult.value = "매뉴얼 텍스트와 질문을 모두 입력해주세요."
            return
        }

        _searchResult.value = "답변 검색 중..."

        val prompt = """
            당신은 전자제품 매뉴얼 분석 전문가입니다.
            제공된 '매뉴얼 텍스트'를 철저히 분석하여 '사용자의 질문'에 대해 가장 정확하고 핵심적인 답변을 요약하여 제공하세요.
            답변은 반드시 질문에 해당하는 **정확한 원인**과 **조치 방법**을 포함하여 명확하게 작성해야 합니다.
            매뉴얼 텍스트에서 관련 정보를 찾을 수 없는 경우에만 '관련 정보를 찾을 수 없습니다.'라고 답변하세요.

            --- 매뉴얼 텍스트 ---
            $manualText
            --- 사용자 질문 ---
            $userQuery
        """.trimIndent()

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                _searchResult.value = response.text ?: "답변 생성 실패."
            } catch (e: Exception) {
                _searchResult.value = "오류 발생: ${e.localizedMessage}"
                e.printStackTrace()
            }
        }
    }
}