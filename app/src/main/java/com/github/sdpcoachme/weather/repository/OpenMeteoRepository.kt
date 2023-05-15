package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.weather.DailyWeather
import com.github.sdpcoachme.weather.Weather
import com.github.sdpcoachme.weather.api.RetrofitClient
import com.github.sdpcoachme.weather.api.WeatherData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class OpenMeteoRepository : WeatherRepository {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }

    override suspend fun loadWeatherForecast(lat: Double, long: Double): Weather {
        val weatherData = RetrofitClient.api.getWeatherData(lat, long, TimeZone.getDefault().id)
        return getCurrentWeather(weatherData)
    }

    private fun getCurrentWeather(data: WeatherData): Weather {
        val dailyWeatherData = data.dailyWeatherData()

        if (dailyWeatherData.isNotEmpty()) {
            val currentWeather = dailyWeatherData[0]
            if (currentWeather != null) {
                return currentWeather.weather
            } else {
                error("getCurrentWeather: current weather was not correctly retrieved")
            }
        } else {
            error("getCurrentWeather: retrieved weather data is empty")
        }
    }

    private fun WeatherData.dailyWeatherData(): Map<Int, DailyWeather> {
        val maxTemperatures = weatherDataObject.maxTemperatures
        val minTemperatures = weatherDataObject.minTemperatures
        val weatherCodes = weatherDataObject.weatherCodes

        return weatherDataObject.days.mapIndexed { index, day ->
            val maxTemperature = maxTemperatures[index]
            val minTemperature = minTemperatures[index]
            val weatherCode = weatherCodes[index]
            Pair(
                index,
                DailyWeather(
                    day = LocalDate.parse(day, DateTimeFormatter.ISO_DATE).dayOfMonth,
                    weather = Weather(
                        weatherCode = weatherCode,
                        maxTemperature = maxTemperature,
                        minTemperature = minTemperature
                    )
                )
            )
        }.toMap()
    }

}