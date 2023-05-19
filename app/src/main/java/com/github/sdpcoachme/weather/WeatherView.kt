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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.R.drawable.weather_cloud_done
import com.github.sdpcoachme.R.drawable.weather_cloud_off
import java.time.LocalDate

/**
 * Displays a WeatherIcon with the maximal and minimal temperature of a given day.
 *
 * @param weatherForecast the weatherForecast to display
 * @param day the day to display
 */
@Composable
fun WeatherView(weatherForecast: MutableState<WeatherForecast>, day: LocalDate) {

    val now = LocalDate.now()
    var dayId = -1

    var weatherText = Pair("?", "?").toWeatherText()
    var weatherCode = weather_cloud_off

    for (i in 0..13) {
        if (day == now.plusDays(i.toLong())) {
            dayId = i
        }
    }

    if (dayId != -1 && weatherForecast.value.forecast.isNotEmpty() &&
        weatherForecast.value.forecast.size > dayId) {

        weatherCode = weatherForecast.value.forecast[dayId].weatherCode
        weatherText = Pair(
            weatherForecast.value.forecast[dayId].maxTemperature.toString(),
            weatherForecast.value.forecast[dayId].minTemperature.toString()
        ).toWeatherText()

    } else if (day.isBefore(now)) {
        weatherCode = weather_cloud_done
        weatherText = Pair("-", "-").toWeatherText()
    }

    WeatherColumn(weatherText, weatherCode)
}

@Composable
private fun WeatherColumn(weatherText: String, weatherCode: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("WEATHER_COLUMN"),
        horizontalAlignment = CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = weatherCode),
            contentDescription = "Weather icon",
            modifier = Modifier.size(35.dp).testTag(weatherCode.toString())
        )
        Text(
            text = weatherText,
            fontSize = 10f.sp,
            modifier = Modifier.testTag(weatherText)
        )
    }
}

fun Pair<String, String>.toWeatherText(): String {
    return "$first | $second"
}

