package com.example.permguide.utils

import android.content.Context
import java.io.File

class CacheManager(private val context: Context) {

    private val fileName = "attractions_cache.json"

    fun saveData(json: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(json)
    }

    fun loadData(): String? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readText() else null
    }
}