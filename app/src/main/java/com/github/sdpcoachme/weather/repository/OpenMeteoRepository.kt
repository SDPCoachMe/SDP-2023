package com.github.sdpcoachme.weather.repository

import com.github.sdpcoachme.R.drawable.*
import com.github.sdpcoachme.weather.Weather
import com.github.sdpcoachme.weather.api.WeatherApi
import com.github.sdpcoachme.weather.api.WeatherData
import java.util.*

/**
 * open-meteo implementation of a WeatherRepository. For further documentation, see
 * WeatherRepository.
 *
 * @param api the weather api to use (open-meteo or MockWeatherApi)
 */
class OpenMeteoRepository(private var api: WeatherApi) : WeatherRepository {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }

    /**
     * Fetches a forecast from the api given a location target.
     *
     * @param lat latitude of the target location
     * @param long longitude of the target location
     */
    override suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        val weatherData = api.getWeatherData(lat, long, TimeZone.getDefault().id)
        return weatherData.parseWeatherData()
    }

    /**
     * Parses a WeatherData to a List<Weather> which will the compose a WeatherForecast.
     * Each weather code of the WeatherData is mapped to a weather icon id.
     */
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

    /**
     * Maps a weather code to a weather icon id.
     */
    private fun Int.toWeatherIcon(): Int {
        return when (this) {
            // clear, mainly clear
            0, 1 -> weather_sunny
            // partly cloudy
            2 -> weather_cloud_sunny
            // overcast, fog
            3, 45, 48 -> weather_cloudy
            // drizzle, rain, freezing rain, rain showers
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82  -> weather_rainy
            // snow fall, snow grains, snow showers
            71, 73, 75, 77, 85, 86 -> weather_snowing
            // slight to heavy thunderstorm
            95, 96, 99 -> weather_thunderstorm
            else ->  weather_sunny

        }
    }

}