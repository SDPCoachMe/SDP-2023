package com.github.sdpcoachme.weather

data class Weather(
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double
)

data class DailyWeather(
    val day: Int,
    val weather: Weather
)

