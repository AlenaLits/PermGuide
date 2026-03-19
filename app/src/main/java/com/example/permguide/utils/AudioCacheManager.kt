package com.example.permguide.utils

import android.content.Context
import java.io.File
import java.net.URL
class AudioCacheManager(private val context: Context) {

    fun getAudioFile(id: Int): File {
        return File(context.filesDir, "audio_$id.mp3")
    }

    fun isAudioCached(id: Int): Boolean {
        return getAudioFile(id).exists()
    }

    fun downloadAudio(id: Int, url: String, onComplete: (File?) -> Unit) {
        Thread {
            try {
                val file = getAudioFile(id)

                val connection = URL(url).openConnection()
                connection.setRequestProperty("Accept", "*/*")
                // 🔥 ВАЖНО — притворяемся браузером
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                )

                connection.connect()

                val contentType = connection.contentType
                android.util.Log.d("AUDIO_TYPE", contentType)

                val input = connection.getInputStream()
                val output = file.outputStream()

                input.copyTo(output)

                output.flush()
                output.close()
                input.close()

                android.util.Log.d("AUDIO_DEBUG", "size after download: ${file.length()}")

                if (file.length() > 10000) { // минимум 10KB
                    onComplete(file)
                } else {
                    file.delete()
                    onComplete(null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }.start()
    }
}