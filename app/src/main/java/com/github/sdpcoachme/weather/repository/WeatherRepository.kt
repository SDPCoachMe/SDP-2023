package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.Weather

/**
 * Repository for fetching weather forecast data.
 */
interface WeatherRepository {

    /**
     * Fetches a weatherForecast from an api.
     *
     * @param lat latitude of the target location
     * @param long longitude of the target location
     */
    suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather>

}