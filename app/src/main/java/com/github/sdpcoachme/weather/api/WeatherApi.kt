package com.github.sdpcoachme.weather.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Weather api to retreive a weather forecast.
 * Sends a https GET request and returns a JSON object.
 */
interface WeatherApi {

    /**
     * Gets two weeks daily weather forecast
     */
    @GET("v1/forecast?daily=weathercode,apparent_temperature_max,apparent_temperature_min&forecast_days=14")
    suspend fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("timezone") timezone: String
    ): WeatherData

}