package com.example.homesavvy

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("CyclePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_CYCLE_ITEMS = "cycle_items"

    fun getCycleItems(): MutableList<CycleItem> {
        val json = prefs.getString(KEY_CYCLE_ITEMS, null)

        Log.d("PrefManager", "Loaded JSON: $json")

        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<CycleItem>>() {}.type
                val items: MutableList<CycleItem> = gson.fromJson(json, type)

                Log.d("PrefManager", "Loaded items count: ${items.size}")

                items
            } catch (e: Exception) {
                Log.e("PrefManager", "Error deserializing JSON: ${e.message}", e)
                mutableListOf()
            }
        } else {
            Log.d("PrefManager", "No cycle items found in preferences.")
            mutableListOf()
        }
    }

    fun addCycleItem(item: CycleItem) {
        val items = getCycleItems()

        items.add(item)

        val json = gson.toJson(items)

        Log.d("PrefManager", "Saving JSON: $json")

        prefs.edit().putString(KEY_CYCLE_ITEMS, json).commit()
    }

    fun removeCycleItem(id: String) {
        val items = getCycleItems()
        items.removeAll { it.id == id }
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_CYCLE_ITEMS, json).apply()
    }
    fun deleteCycleItem(itemId: String) {
        val currentList = getCycleItems().toMutableList()

        val itemToRemove = currentList.find { it.id == itemId }

        if (itemToRemove != null) {
            currentList.remove(itemToRemove)

            val json = gson.toJson(currentList)
            prefs.edit().putString(KEY_CYCLE_ITEMS, json).apply()
        }
    }
}