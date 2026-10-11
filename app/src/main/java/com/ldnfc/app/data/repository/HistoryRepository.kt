package com.ldnfc.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ldnfc.app.data.model.WriteHistoryItem

/**
 * Repositório para persistência do histórico de gravações usando SharedPreferences e Gson.
 */
class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("lego_dimensions_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyKey = "write_history_list"

    fun getHistory(): List<WriteHistoryItem> {
        val json = prefs.getString(historyKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WriteHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistoryItem(item: WriteHistoryItem) {
        val currentList = getHistory().toMutableList()
        currentList.add(0, item) // Adiciona no início
        // Limita a 100 itens no histórico
        val trimmed = if (currentList.size > 100) currentList.take(100) else currentList
        val json = gson.toJson(trimmed)
        prefs.edit().putString(historyKey, json).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(historyKey).apply()
    }
}
