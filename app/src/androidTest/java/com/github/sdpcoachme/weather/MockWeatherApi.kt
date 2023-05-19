package com.github.sdpcoachme.weather

import com.github.sdpcoachme.weather.api.WeatherApi
import com.github.sdpcoachme.weather.api.WeatherData
import com.github.sdpcoachme.weather.api.WeatherDataObject

class MockWeatherApi : WeatherApi {

    companion object {
        val MOCK_WEATHER_CODES: List<Int> = listOf(80,61,3,3,3,80,80,80,80,55,51,51,53,80)
        val MOCK_MAX_TEMP: List<Double> = listOf(13.7,17.1,20.6,22.0,25.4,24.1,22.9,22.3,18.3,20.6,21.2,23.3,25.5,28.4)
        val MOCK_MIN_TEMP: List<Double> = listOf(4.0,6.3,8.8,12.4,14.3,14.8,16.1,15.2,10.1,10.6,11.4,12.9,12.7,14.2)
        var MOCK_DAYS: List<String> = listOf("2056-08-01","2056-08-02","2056-08-03","2056-08-04",
            "2056-08-05","2056-08-06","2056-08-07","2056-08-08","2056-08-09","2056-08-10",
            "2056-08-11","2056-08-12","2056-08-13","2056-08-14")
    }

    private var mockWeatherData: WeatherData = WeatherData(WeatherDataObject(
        MOCK_DAYS,
        MOCK_WEATHER_CODES,
        MOCK_MAX_TEMP,
        MOCK_MIN_TEMP
    ))

    override suspend fun getWeatherData(lat: Double, long: Double, timezone: String): WeatherData {
        return mockWeatherData
    }

    fun withMockWeatherData(weatherData: WeatherData): MockWeatherApi {
        mockWeatherData = weatherData
        return this
    }

}