package com.example.permguide.data

import android.content.Context
import com.example.permguide.model.Attraction
import com.example.permguide.network.RetrofitClient
import com.example.permguide.utils.CacheManager
import com.example.permguide.utils.SettingsManager
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataRepository(private val context: Context) {

    private val settings = SettingsManager(context)
    private val cache = CacheManager(context)

    fun getAttractions(callback: (List<Attraction>) -> Unit) {

        if (settings.offlineMode) {
            // ОФЛАЙН
            val cached = cache.loadData()

            if (cached != null) {
                val list = Gson().fromJson(cached, Array<Attraction>::class.java).toList()
                callback(list)
            } else {
                callback(emptyList())
            }

        } else {
            // ОНЛАЙН
            RetrofitClient.instance.getAttractions()
                .enqueue(object : Callback<List<Attraction>> {

                    override fun onResponse(
                        call: Call<List<Attraction>>,
                        response: Response<List<Attraction>>
                    ) {
                        val data = response.body() ?: emptyList()

                        // сохраняем
                        val json = Gson().toJson(data)
                        cache.saveData(json)

                        callback(data)
                    }

                    override fun onFailure(call: Call<List<Attraction>>, t: Throwable) {

                        // fallback → берем кэш
                        val cached = cache.loadData()

                        if (cached != null) {
                            val list = Gson().fromJson(cached, Array<Attraction>::class.java).toList()
                            callback(list)
                        } else {
                            callback(emptyList())
                        }
                    }
                })
        }
    }
}