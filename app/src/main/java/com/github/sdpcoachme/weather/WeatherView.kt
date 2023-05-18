package com.github.sdpcoachme.weather

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun WeatherView(weatherForecast: MutableState<WeatherForecast>) {

    if (weatherState.value.isNotEmpty()) {
        Text(
            text = "weatherCodes = ${weatherState.value[0].weatherCode} " +
                    "${weatherState.value[1].weatherCode} " +
                    "${weatherState.value[2].weatherCode} " +
                    "${weatherState.value[3].weatherCode} " +
                    "${weatherState.value[4].weatherCode} " +
                    "${weatherState.value[5].weatherCode} " +
                    "${weatherState.value[6].weatherCode} " +
                    "${weatherState.value[7].weatherCode} " +
                    "${weatherState.value[8].weatherCode} " +
                    "${weatherState.value[9].weatherCode} " +
                    "${weatherState.value[10].weatherCode} " +
                    "${weatherState.value[11].weatherCode} " +
                    "${weatherState.value[12].weatherCode} " +
                    "${weatherState.value[13].weatherCode} " +
                    "maxTemperatures = ${weatherState.value[0].maxTemperature} " +
                    "${weatherState.value[1].maxTemperature} " +
                    "${weatherState.value[2].maxTemperature} " +
                    "${weatherState.value[3].maxTemperature} " +
                    "${weatherState.value[4].maxTemperature} " +
                    "${weatherState.value[5].maxTemperature} " +
                    "${weatherState.value[6].maxTemperature} " +
                    "${weatherState.value[7].maxTemperature} " +
                    "${weatherState.value[8].maxTemperature} " +
                    "${weatherState.value[9].maxTemperature} " +
                    "${weatherState.value[10].maxTemperature} " +
                    "${weatherState.value[11].maxTemperature} " +
                    "${weatherState.value[12].maxTemperature} " +
                    "${weatherState.value[13].maxTemperature} " +
                    "minTemperatures = ${weatherState.value[0].minTemperature} " +
                    "${weatherState.value[1].minTemperature} " +
                    "${weatherState.value[2].minTemperature} " +
                    "${weatherState.value[3].minTemperature} " +
                    "${weatherState.value[4].minTemperature} " +
                    "${weatherState.value[5].minTemperature} " +
                    "${weatherState.value[6].minTemperature} " +
                    "${weatherState.value[7].minTemperature} " +
                    "${weatherState.value[8].minTemperature} " +
                    "${weatherState.value[9].minTemperature} " +
                    "${weatherState.value[10].minTemperature} " +
                    "${weatherState.value[11].minTemperature} " +
                    "${weatherState.value[12].minTemperature} " +
                    "${weatherState.value[13].minTemperature} "
        )
    } else {
        Text(text = "LOADING....")
    }

}