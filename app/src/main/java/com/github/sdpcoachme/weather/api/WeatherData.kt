package com.github.sdpcoachme.weather.api

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("temperature_2m")
    val temperatures: List<Double>,
    @SerializedName("weathercode")
    val weatherCodes: List<Int>
)
