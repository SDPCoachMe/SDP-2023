package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.R
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
                weatherCode = weatherCode.toWeatherIcon(),
                maxTemperature = maxTemperature,
                minTemperature = minTemperature
            )
        }
    }

    private fun Int.toWeatherIcon(): Int {
        return when (this) {
            // clear, mainly clear
            0, 1 -> R.drawable.weather_sunny
            // partly cloudy
            2 -> R.drawable.weather_cloud_sunny
            // overcast, fog
            3, 45, 48 -> R.drawable.weather_cloudy
            // drizzle, rain, freezing rain, rain showers
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82  -> R.drawable.weather_rainy
            // snow fall, snow grains, snow showers
            71, 73, 75, 77, 85, 86 -> R.drawable.weather_snowing
            // slight to heavy thunderstorm
            95, 96, 99 -> R.drawable.weather_thunderstorm
            else ->  R.drawable.weather_sunny

        }
    }

}