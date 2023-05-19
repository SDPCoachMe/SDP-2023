package com.github.sdpcoachme.weather.api

import com.google.gson.annotations.SerializedName

/**
 * Data type to parse a JSON object to a Weather object.
 */
data class WeatherData(
    @SerializedName("daily")
    val weatherDataObject: WeatherDataObject
)

data class WeatherDataObject(
    @SerializedName("time")
    val days: List<String>,
    @SerializedName("weathercode")
    val weatherCodes: List<Int>,
    @SerializedName("apparent_temperature_max")
    val maxTemperatures: List<Double>,
    @SerializedName("apparent_temperature_min")
    val minTemperatures: List<Double>
)
