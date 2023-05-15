package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.Weather

/**
 * Repository for fetching weather forecast data.
 */
interface WeatherRepository {

    suspend fun loadWeatherForecast(lat: Double, long: Double): Weather

}