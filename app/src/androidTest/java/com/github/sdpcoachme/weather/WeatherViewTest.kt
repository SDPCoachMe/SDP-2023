package com.github.sdpcoachme.weather

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.R.drawable.*
import com.github.sdpcoachme.weather.MockWeatherRepository.Companion.MOCK_FORECAST
import com.github.sdpcoachme.weather.MockWeatherRepository.Companion.MOCK_WEATHER
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WeatherViewTest {

    companion object {

        val WEATHER_OFF_TEXT = Pair("?", "?").toWeatherText()
        val WEATHER_DONE_TEXT = Pair("-", "-").toWeatherText()
        val MOCK_WEATHER_TEXT = Pair("20.0", "10.0").toWeatherText()
        const val WEATHER_CLOUD_OFF = weather_cloud_off.toString()
        const val WEATHER_CLOUD_DONE = weather_cloud_done.toString()
        const val WEATHER_SUNNY = weather_sunny.toString()

        val NOW_DATE: LocalDate = LocalDate.now()
        val RANDOM_DATE: LocalDate = LocalDate.of(2056, 5, 16)

    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun weatherViewWorksOn(
        forecast: List<Weather>,
        day: LocalDate,
        text: String,
        code: String
    ) {
        composeTestRule.setContent {
            WeatherView(
                weatherForecast = mutableStateOf(WeatherForecast(forecast)),
                day = day
            )
        }
        composeTestRule.onNodeWithTag(text).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(code).assertExists().assertIsDisplayed()
    }

    @Test
    fun weatherViewWorksOnEmptyForecast() {
        weatherViewWorksOn(
            forecast = emptyList(),
            day = RANDOM_DATE,
            text = WEATHER_OFF_TEXT,
            code = WEATHER_CLOUD_OFF
        )
    }

    @Test
    fun weatherViewWorksOnPastDate() {
        weatherViewWorksOn(
            forecast = emptyList(),
            day = NOW_DATE.minusDays(5),
            text = WEATHER_DONE_TEXT,
            code = WEATHER_CLOUD_DONE
        )
    }

    @Test
    fun weatherViewIgnoresForecastForOuterDates() {
        weatherViewWorksOn(
            forecast = MOCK_FORECAST.subList(0, 7),
            day = NOW_DATE.plusDays(10),
            text = WEATHER_OFF_TEXT,
            code = WEATHER_CLOUD_OFF
        )
    }

    @Test
    fun weatherViewDisplaysCorrectWeatherOnGivenDay() {
        weatherViewWorksOn(
            forecast = MOCK_FORECAST,
            day = NOW_DATE.plusDays(5),
            text = WEATHER_SUNNY,
            code = MOCK_WEATHER_TEXT
        )
    }

    @Test
    fun weatherViewRecomposesOnWeatherStateChange() {
        val weatherState = mutableStateOf(WeatherForecast(emptyList()))
        composeTestRule.setContent {
            WeatherView(
                weatherForecast = weatherState,
                day = NOW_DATE
            )
        }
        composeTestRule.onNodeWithTag(WEATHER_CLOUD_OFF).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(WEATHER_OFF_TEXT).assertExists().assertIsDisplayed()
        weatherState.value = WeatherForecast(MOCK_FORECAST)
        composeTestRule.onNodeWithTag(WEATHER_SUNNY).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(MOCK_WEATHER_TEXT).assertExists().assertIsDisplayed()
    }

    @Test
    fun weatherViewDoesNotDisplayAboveTwoWeeksForecast() {
        weatherViewWorksOn(
            forecast = MOCK_FORECAST.plus(MOCK_WEATHER),
            day = NOW_DATE.plusDays(14),
            text = WEATHER_OFF_TEXT,
            code = WEATHER_CLOUD_OFF
        )
        composeTestRule.onNodeWithTag(WEATHER_SUNNY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MOCK_WEATHER_TEXT).assertDoesNotExist()
    }

}