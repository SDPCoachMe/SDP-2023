package com.github.sdpcoachme.weather

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun WeatherView(weatherForecast: MutableState<WeatherForecast>, day: LocalDate) {

    val localDate = LocalDate.now()
    var dayId = -1
    for (i in 0..13) {
        if (day == localDate.plusDays(i.toLong())) {
            dayId = i
        }
    }

    if (dayId != -1 && weatherForecast.value.forecast.isNotEmpty() &&
        weatherForecast.value.forecast.size > dayId) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = weatherForecast.value.forecast[dayId].weatherCode),
                contentDescription = "Weather icon",
                modifier = Modifier.size(35.dp)
            )
            Text(text = weatherForecast.value.forecast[dayId].maxTemperature.toString() + " | " +
                    weatherForecast.value.forecast[dayId].minTemperature.toString(),
                fontSize = 10f.sp)
        }

    } else {
        Text(text = "-")
        // TODO handle here
    }

}