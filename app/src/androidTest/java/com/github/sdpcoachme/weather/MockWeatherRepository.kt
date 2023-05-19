package com.github.sdpcoachme.weather

import com.github.sdpcoachme.R
import com.github.sdpcoachme.weather.repository.WeatherRepository

class MockWeatherRepository : WeatherRepository {

    companion object {

        val MOCK_WEATHER = Weather(R.drawable.weather_sunny, 20.0, 10.0)
        val MOCK_FORECAST = List(14) { MOCK_WEATHER }

    }

    override suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        return MOCK_FORECAST
    }

}