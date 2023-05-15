package com.github.sdpcoachme.weather.api

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("hourly")
    val weatherDataObject: WeatherDataObject
)

data class WeatherDataObject(
    @SerializedName("temperature_2m")
    val temperatures: List<Double>,
    @SerializedName("weathercode")
    val weatherCodes: List<Int>
)
