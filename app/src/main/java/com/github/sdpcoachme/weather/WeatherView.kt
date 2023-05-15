package com.github.sdpcoachme.weather

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.github.sdpcoachme.weather.WeatherState.*

@Composable
fun WeatherView(weatherState: WeatherState) {
    when (weatherState) {
        is LoadingState -> {
            // loading view
            Text(text = "Loading State")
        }
        is DataState -> {
            // data view with weatherState.data
            Text(text = "Data State ${weatherState.data}")
        }
        is ErrorState -> {
            // error view with weatherState.error
            Text(text = "Error State ${weatherState.error}")
        }
    }
}