package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.api.WeatherData

/**
 * Repository for fetching weather forecast data.
 */
interface WeatherRepository {

    suspend fun loadWeatherForecast(lat: Double, long: Double): WeatherData

}