package com.github.sdpcoachme.weather

data class Weather(
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double
)

data class WeatherForecast(
    val weathers: List<Weather>
) {
    // Constructor needed to make the data class serializable
    constructor() : this(emptyList())
}

