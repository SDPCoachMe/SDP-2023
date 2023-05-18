package com.github.sdpcoachme.weather

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.weather.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class WeatherPresenter {

    private lateinit var weatherRepository: WeatherRepository

    var observableWeatherForecast: MutableState<WeatherForecast> = mutableStateOf(WeatherForecast())

    fun bind(weatherRepository: WeatherRepository): WeatherPresenter {
        this.weatherRepository = weatherRepository
        return this
    }

    fun getWeatherForecast(lat: Double, long: Double): CompletableFuture<WeatherForecast> {
        val weatherForecastFuture = CompletableFuture<WeatherForecast>()

        CoroutineScope(Job()).launch {
            observableWeatherForecast.value = WeatherForecast(loadWeatherForecast(lat, long))
            weatherForecastFuture.complete(observableWeatherForecast.value)
            cancel()
        }
        return weatherForecastFuture
    }

    private suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        return weatherRepository.loadWeatherForecast(lat, long)
    }

}