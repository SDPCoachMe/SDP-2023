package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.api.RetrofitClient
import com.github.sdpcoachme.weather.api.WeatherData

class OpenMeteoRepository: WeatherRepository {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    override suspend fun loadWeatherForecast(lat: Double, long: Double): WeatherData {
        return RetrofitClient.api.getWeatherData(lat, long)
    }

}