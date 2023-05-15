package com.github.sdpcoachme.weather

sealed class WeatherState {
    object LoadingState: WeatherState()
    data class DataState(val weather: Weather): WeatherState()
    data class ErrorState(val error: String): WeatherState()
}