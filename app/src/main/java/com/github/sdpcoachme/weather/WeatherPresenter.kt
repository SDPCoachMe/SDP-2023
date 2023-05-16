package com.github.sdpcoachme.weather

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.weather.repository.WeatherRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WeatherPresenter {

    private lateinit var weatherRepository: WeatherRepository

    private val scope = CoroutineScope(Job())
    var weatherState: MutableState<List<Weather>> = mutableStateOf(listOf())

    fun bind(weatherRepository: WeatherRepository, target: LatLng) {
        this.weatherRepository = weatherRepository
        getWeatherForecast(target.latitude, target.longitude)
    }

    fun unbind() {
        scope.cancel()
    }

    private fun getWeatherForecast(lat: Double, long: Double) {
        scope.launch {
            weatherState.value = loadWeatherForecast(lat, long)
            // TODO handle return here
        }
    }

    private suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        return weatherRepository.loadWeatherForecast(lat, long)
    }

}