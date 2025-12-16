package com.example.homesavvy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class RecommendationListActivity : AppCompatActivity() {

    private lateinit var llPartsList: LinearLayout
    private lateinit var tvNoData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation_list)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_recommendation)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        llPartsList = findViewById(R.id.ll_parts_list)
        tvNoData = findViewById(R.id.tv_no_data)

        loadAndDisplayRecommendations()
    }

    private fun loadAndDisplayRecommendations() {
        llPartsList.removeAllViews()
        val sharedPrefs = getSharedPreferences("HomeSavvyPrefs", Context.MODE_PRIVATE)

        val partsString = sharedPrefs.getString("LAST_PARTS_LIST", "") ?: ""
        val toolsString = sharedPrefs.getString("LAST_TOOLS_LIST", "") ?: ""

        if (partsString.isEmpty() && toolsString.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            return
        }

        tvNoData.visibility = View.GONE

        val partsAndTools = mutableListOf<String>()

        if (partsString.isNotEmpty()) {
            partsAndTools.addAll(
                partsString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            )
        }
        if (toolsString.isNotEmpty()) {
            partsAndTools.addAll(
                toolsString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            )
        }

        if (partsAndTools.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            return
        }

        val inflater = LayoutInflater.from(this)

        partsAndTools.forEach { item ->

            val itemLayout = inflater.inflate(R.layout.item_recommendation_list_layout, llPartsList, false)
            val itemName = itemLayout.findViewById<TextView>(R.id.tv_item_name)
            val cartButton = itemLayout.findViewById<ImageButton>(R.id.btn_search_link)

            if (item.isNotBlank()) {
                itemName.text = "   ✔  ${item}"

                cartButton.setOnClickListener {
                    searchForPurchase(item)
                }

                llPartsList.addView(itemLayout)
            }
        }
    }

    private fun searchForPurchase(itemName: String) {
        val searchUrl = "https://www.google.com/search?q=$itemName 구매 오프라인 최저가"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}