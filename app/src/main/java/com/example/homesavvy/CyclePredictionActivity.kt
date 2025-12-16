package com.example.homesavvy

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.util.*
import java.text.SimpleDateFormat
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import java.util.Date
import java.util.Locale
import android.view.ViewGroup
import android.util.Log
import android.graphics.Typeface
import android.text.InputType
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog

class CyclePredictionActivity : AppCompatActivity() {

    private lateinit var etLastChangeDate: EditText
    private lateinit var spItemType: Spinner
    private lateinit var btnPredictAndSave: Button
    private lateinit var llCycleListContainer: LinearLayout
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cycle_prediction)

        preferenceManager = PreferenceManager(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_cycle_prediction)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etLastChangeDate = findViewById(R.id.et_last_change_date)
        spItemType = findViewById(R.id.sp_item_type)
        btnPredictAndSave = findViewById(R.id.btn_predict_and_save)
        llCycleListContainer = findViewById(R.id.ll_cycle_list_container)

        etLastChangeDate.inputType = InputType.TYPE_NULL
        etLastChangeDate.isFocusable = false

        etLastChangeDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnPredictAndSave.setOnClickListener {
            predictAndSaveCycle()
        }

        displayCycleItems()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            etLastChangeDate.setText(sdf.format(calendar.time))
        }

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerDialog,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setOnShowListener { dialog ->
            val d = dialog as DatePickerDialog

            d.getButton(DatePickerDialog.BUTTON_POSITIVE).text = "확인"
            d.getButton(DatePickerDialog.BUTTON_POSITIVE).setOnClickListener {
                dateSetListener.onDateSet(d.datePicker,
                    d.datePicker.year,
                    d.datePicker.month,
                    d.datePicker.dayOfMonth)
                d.dismiss()
            }

            d.getButton(DatePickerDialog.BUTTON_NEGATIVE).text = "취소"
            d.getButton(DatePickerDialog.BUTTON_NEGATIVE).setOnClickListener {
                d.dismiss()
            }

            val buttonColor = resources.getColor(R.color.color_primary_deep_slate, theme)
            d.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(buttonColor)
            d.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(buttonColor)
        }

        datePickerDialog.show()
    }

    private fun predictAndSaveCycle() {
        Log.d("CycleDebug", "predictAndSaveCycle 함수 시작")

        val lastDateStr = etLastChangeDate.text.toString()
        val itemType = spItemType.selectedItem.toString()

        Log.d("CycleDebug", "Last Date: '$lastDateStr', Item Type: '$itemType'")

        if (lastDateStr.isEmpty() || itemType.isEmpty() || itemType == "소모품 선택") {
            Log.e("CycleDebug", "필수 입력값이 비어있어 함수 종료.")
            Toast.makeText(this, "교체일과 소모품 종류를 모두 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastChangeDate = sdf.parse(lastDateStr)

        if (lastChangeDate == null) {
            Log.e("CycleDebug", "날짜 형식 오류로 함수 종료.")
            Toast.makeText(this, "날짜 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. 예측 날짜 계산
        val averageLifespanDays = when (itemType) {
            "정수기 필터" -> 180L
            "에어컨 필터" -> 365L
            "건전지" -> 90L
            "테스트" -> 1L
            else -> 180L
        }

        val predictionCalendar = Calendar.getInstance().apply {
            time = lastChangeDate
            add(Calendar.DAY_OF_YEAR, averageLifespanDays.toInt())
        }

        val predictedNextDateStr = sdf.format(predictionCalendar.time)

        val uniqueId = System.currentTimeMillis().toString()

        scheduleNotification(
            predictionCalendar.timeInMillis,
            itemType,
            uniqueId
        )

        val newItem = CycleItem(
            id = uniqueId,
            itemType = itemType,
            lastChangeDate = lastDateStr,
            predictedNextDate = predictedNextDateStr
        )
        preferenceManager.addCycleItem(newItem)

        Toast.makeText(this,
            "${itemType}의 예측 교체일은 ${predictedNextDateStr}입니다. 알림이 예약되었습니다.",
            Toast.LENGTH_LONG
        ).show()

        displayCycleItems()
    }

    private fun deleteCycleItem(item: CycleItem) {
        val requestCode = item.id.hashCode()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent) // 예약된 알람 취소

        preferenceManager.deleteCycleItem(item.id)

        displayCycleItems()

        Toast.makeText(this, "${item.itemType} 알림이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        Log.d("CyclePrediction", "알림 삭제 성공: ${item.itemType}, ID: ${item.id}")
    }

    private fun displayCycleItems() {
        llCycleListContainer.removeAllViews()
        val items = preferenceManager.getCycleItems()

        Log.d("CycleDebug", "Items loaded for display: ${items.size}")

        if (items.isEmpty()) {
            val tv = TextView(this).apply {
                text = "현재 예약된 교체 알림이 없습니다."
                padding(16)
                textSize = 16f
            }
            llCycleListContainer.addView(tv)
        } else {
            items.forEachIndexed { index, item ->
                Log.d("CycleDebug", "Drawing item $index: ${item.itemType}")
                llCycleListContainer.addView(createCycleItemView(item))
            }
        }
    }

    private fun createCycleItemView(item: CycleItem): LinearLayout {

        val context = this

        val layout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            }
            orientation = LinearLayout.VERTICAL
            try {
                setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            } catch (e: Exception) {
                Log.e("CycleCrash", "White color resource error: ${e.message}")
                setBackgroundColor(android.graphics.Color.WHITE)
            }
            padding(16)
        }

        val tvName = TextView(context).apply {
            text = "✔️ ${item.itemType}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            padding(0, 0, 0, 4)
            setTextColor(ContextCompat.getColor(context, R.color.color_primary_deep_slate))
        }

        val infoContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            weightSum = 1f
        }

        val tvDates = TextView(context).apply {
            text = "마지막 교체: ${item.lastChangeDate} | 예측 교체일: ${item.predictedNextDate}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.color_accent_general))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f)
        }

        val btnDelete = Button(context).apply {
            text = "삭제"
            textSize = 14f
            try {
                setBackgroundColor(ContextCompat.getColor(context, R.color.red_delete))
            } catch (e: Exception) {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            setTextColor(ContextCompat.getColor(context, android.R.color.white))

            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.3f).apply {
                setMargins((16 * resources.displayMetrics.density).toInt(), 0, 0, 0)
            }

            setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("알림 삭제 확인")
                    .setMessage("${item.itemType} 교체 알림을 삭제하시겠습니까? 예약된 알람도 함께 취소됩니다.")
                    .setPositiveButton("삭제") { dialog, which ->
                        deleteCycleItem(item)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }

        infoContainer.addView(tvDates)
        infoContainer.addView(btnDelete)

        layout.addView(tvName)
        layout.addView(infoContainer)

        Log.d("CycleDebug", "Item View successfully created with Delete button.")
        return layout
    }

    private fun scheduleNotification(timeInMillis: Long, itemType: String, uniqueId: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "정확한 알람 권한이 없습니다. 사용자에게 권한 요청이 필요합니다.")
                return
            }
        }

        val requestCode = uniqueId.hashCode()

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("NOTIFICATION_TITLE", "HomeSavvy 교체 알림")
            putExtra("NOTIFICATION_MESSAGE", "지금 ${itemType} 교체 시기입니다! Parts & Tools Finder를 확인하세요.")
            putExtra("NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val reservedTime = sdf.format(Date(timeInMillis))
        Log.d("CyclePrediction", "알림 예약 성공: $itemType, ID: $uniqueId, 시간: $reservedTime")
    }

    private fun TextView.padding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        setPadding(
            (left * resources.displayMetrics.density).toInt(),
            (top * resources.displayMetrics.density).toInt(),
            (right * resources.displayMetrics.density).toInt(),
            (bottom * resources.displayMetrics.density).toInt()
        )
    }
    private fun LinearLayout.padding(all: Int) {
        val p = (all * resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
    }
}