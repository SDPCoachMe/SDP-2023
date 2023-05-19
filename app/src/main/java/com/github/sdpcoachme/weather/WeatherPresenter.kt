package com.github.sdpcoachme.weather

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.weather.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * Presenter that makes the communication between WeatherViews/CachingStore and a WeatherRepository.
 * Provides an observableWeatherState and a cache-ready weatherForecast.
 */
class WeatherPresenter {

    private lateinit var weatherRepository: WeatherRepository

    var observableWeatherForecast: MutableState<WeatherForecast> = mutableStateOf(WeatherForecast())

    /**
     * Instantiate the presenter with a WeatherRepository.
     *
     * @param weatherRepository the repository to use
     */
    fun bind(weatherRepository: WeatherRepository): WeatherPresenter {
        this.weatherRepository = weatherRepository
        return this
    }

    /**
     * Fetches a weather forecast from the repository and updates the observableWeatherForecast.
     * This is run asynchronously and returns a future holding the new WeatherForecast.
     *
     * @param lat latitude of the target location
     * @param long longitude of the target location
     */
    fun getWeatherForecast(lat: Double, long: Double): CompletableFuture<WeatherForecast> {
        val weatherForecastFuture = CompletableFuture<WeatherForecast>()

        CoroutineScope(Job()).launch {
            val newWeatherForecast = WeatherForecast(loadWeatherForecast(lat, long))
            observableWeatherForecast.value = newWeatherForecast
            weatherForecastFuture.complete(newWeatherForecast)
            cancel()
        }
        return weatherForecastFuture
    }

    private suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        return weatherRepository.loadWeatherForecast(lat, long)
    }

}