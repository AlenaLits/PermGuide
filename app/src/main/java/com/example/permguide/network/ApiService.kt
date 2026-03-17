package com.example.permguide.network


import com.example.permguide.model.Attraction
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("places")
    fun getAttractions(): Call<List<Attraction>>
}