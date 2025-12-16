package com.example.homesavvy

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


const val REPLACEMENT_WARNING_DAYS = 7L

fun analyzeCycleData(context: Context): SummaryData {
    val preferenceManager = PreferenceManager(context)
    val cycleItems = preferenceManager.getCycleItems()

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val timeLimit = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(REPLACEMENT_WARNING_DAYS)

    val imminentItems = mutableListOf<String>()

    for (item in cycleItems) {
        try {
            val predictedDate = sdf.parse(item.predictedNextDate)

            if (predictedDate != null && predictedDate.time <= timeLimit) {
                imminentItems.add(item.itemType)
            }
        } catch (e: Exception) {
            Log.e("SummaryAnalysis", "날짜 파싱 오류: ${item.itemType}", e)
        }
    }

    val groupedItems = imminentItems.groupingBy { it }.eachCount()
    val replacementSummary = groupedItems.map { "${it.key} ${it.value}건" }

    return SummaryData(imminentReplacement = replacementSummary, urgentCheckNeeded = emptyList())
}