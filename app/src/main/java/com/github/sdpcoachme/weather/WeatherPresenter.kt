package com.github.sdpcoachme.weather

import com.github.sdpcoachme.weather.api.WeatherData
import com.github.sdpcoachme.weather.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WeatherPresenter {

    private lateinit var weatherRepository: WeatherRepository

    private val scope = CoroutineScope(Job())
    var weatherState: WeatherState = WeatherState.LoadingState

    fun bind(weatherRepository: WeatherRepository) {
        this.weatherRepository = weatherRepository
    }

    fun unbind() {
        scope.cancel()
    }

    private fun getWeatherForecast(lat: Double, long: Double) {
        scope.launch {
            weatherState = WeatherState.DataState(loadWeatherForecast(lat, long))
            // TODO handle return here
        }
    }

    private suspend fun loadWeatherForecast(lat: Double, long: Double): WeatherData {
        return weatherRepository.loadWeatherForecast(lat, long)
    }

}