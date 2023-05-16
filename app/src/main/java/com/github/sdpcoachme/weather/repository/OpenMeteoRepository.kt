package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.Weather
import com.github.sdpcoachme.weather.api.RetrofitClient
import com.github.sdpcoachme.weather.api.WeatherData
import java.util.TimeZone

class OpenMeteoRepository : WeatherRepository {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }

    override suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        val weatherData = RetrofitClient.api.getWeatherData(lat, long, TimeZone.getDefault().id)
        return weatherData.parseWeatherData()
    }

    private fun WeatherData.parseWeatherData(): List<Weather> {
        val maxTemperatures = weatherDataObject.maxTemperatures
        val minTemperatures = weatherDataObject.minTemperatures
        val weatherCodes = weatherDataObject.weatherCodes

        return List(weatherDataObject.days.size) { index ->
            val maxTemperature = maxTemperatures[index]
            val minTemperature = minTemperatures[index]
            val weatherCode = weatherCodes[index]

            Weather(
                weatherCode = weatherCode,
                maxTemperature = maxTemperature,
                minTemperature = minTemperature
            )
        }
    }

}