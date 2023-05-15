package com.github.sdpcoachme.weather

import com.github.sdpcoachme.weather.api.WeatherData

sealed class WeatherState {
    object LoadingState: WeatherState()
    data class DataState(val data: WeatherData): WeatherState()
    data class ErrorState(val error: String): WeatherState()
}