package com.example.permguide.model

data class Attraction(
    val idAttraction: Int,
    val nameAttraction: String,
    val descriptionAttraction: String,
    val latitudeAttraction: Double,
    val longitudeAttraction: Double,
    val categoryAttraction: Int,
    val photo: String?,
    val audio: String?
)
