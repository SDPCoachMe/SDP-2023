package com.github.sdpcoachme.weather

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.weather.repository.WeatherRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WeatherPresenter(private val cachingStore: CachingStore) {

    private lateinit var weatherRepository: WeatherRepository

    private val scope = CoroutineScope(Job())
    var observableWeatherForecast: MutableState<WeatherForecast> = mutableStateOf(WeatherForecast())

    fun bind(weatherRepository: WeatherRepository, target: LatLng): WeatherPresenter {
        this.weatherRepository = weatherRepository
        getWeatherForecast(target.latitude, target.longitude)
        return this
    }

    fun unbind() {
        scope.cancel()
    }

    private fun getWeatherForecast(lat: Double, long: Double) {
        scope.launch {
            observableWeatherForecast.value = WeatherForecast(loadWeatherForecast(lat, long))
            cachingStore.setWeatherForecast(observableWeatherForecast.value)
        }
    }

    private suspend fun loadWeatherForecast(lat: Double, long: Double): List<Weather> {
        return weatherRepository.loadWeatherForecast(lat, long)
    }

}