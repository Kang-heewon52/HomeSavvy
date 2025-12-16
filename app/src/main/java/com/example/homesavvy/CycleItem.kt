package com.example.homesavvy

data class CycleItem (
    val id: String,
    val itemType: String,
    val lastChangeDate: String,
    val predictedNextDate: String
)
data class SummaryData(
    val imminentReplacement: List<String>,
    val urgentCheckNeeded: List<String>
)