package com.github.sdpcoachme.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.R.drawable.*
import com.github.sdpcoachme.weather.MockWeatherApi.Companion.MOCK_DAYS
import com.github.sdpcoachme.weather.MockWeatherApi.Companion.MOCK_MAX_TEMP
import com.github.sdpcoachme.weather.MockWeatherApi.Companion.MOCK_MIN_TEMP
import com.github.sdpcoachme.weather.MockWeatherRepository.Companion.MOCK_FORECAST
import com.github.sdpcoachme.weather.api.RetrofitClient
import com.github.sdpcoachme.weather.api.WeatherData
import com.github.sdpcoachme.weather.api.WeatherDataObject
import com.github.sdpcoachme.weather.repository.OpenMeteoRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WeatherTest {

    @Test
    fun weatherPresenterCorrectlyRetrievesApiWeatherForecast() {
        val presenter = WeatherPresenter().bind(OpenMeteoRepository(RetrofitClient.api))
        presenter.getWeatherForecast(0.0, 0.0).get(1, TimeUnit.SECONDS).let  { weatherForecast ->
            assertThat(weatherForecast, `is`(notNullValue()))
            assertThat(weatherForecast.forecast, not(`is`(emptyList())))
        }
    }

    @Test
    fun weatherPresenterCorrectlyUpdatesObservableApiWeatherState() {
        val presenter = WeatherPresenter().bind(OpenMeteoRepository(RetrofitClient.api))
        val previous = presenter.observableWeatherForecast.value
        presenter.getWeatherForecast(0.0, 0.0).get(1, TimeUnit.SECONDS).let  {
            assertThat(presenter.observableWeatherForecast.value, not(`is`(previous)))
        }
    }

    @Test
    fun weatherPresenterLoadsCorrectWeatherForecast() {
        val presenter = WeatherPresenter().bind(MockWeatherRepository())
        presenter.getWeatherForecast(0.0, 0.0).get(1, TimeUnit.SECONDS).let  { weatherForecast ->
            assertThat(weatherForecast, `is`(WeatherForecast(MOCK_FORECAST)))
        }
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class)
    fun openMeteoRepositoryCorrectlyLoadsWeatherForecast() {
        val repository = OpenMeteoRepository(RetrofitClient.api)
        GlobalScope.launch {
            val forecast = repository.loadWeatherForecast(0.0, 0.0)
            assertThat(forecast, `is`(notNullValue()))
            assertThat(forecast, not(`is`(emptyList())))
            cancel()
        }
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class)
    fun openMeteoRepositoryMapsWeatherCodeToWeatherIcon() {
        for (i in 0..7) {
            GlobalScope.launch {
                val data = WeatherData(WeatherDataObject(
                    MOCK_DAYS,
                    (i*14..((i+1)*14)).toList(),
                    MOCK_MAX_TEMP,
                    MOCK_MIN_TEMP
                ))
                val api = MockWeatherApi().withMockWeatherData(data)
                val forecast = OpenMeteoRepository(api).loadWeatherForecast(0.0, 0.0)
                assertThat(forecast, `is`(notNullValue()))
                assertThat(forecast, not(`is`(emptyList())))

                forecast.forEach { weather ->
                    assertThat(weather.weatherCode, anyOf(
                        `is`(weather_sunny),
                        `is`(weather_cloud_sunny),
                        `is`(weather_cloudy),
                        `is`(weather_rainy),
                        `is`(weather_snowing),
                        `is`(weather_thunderstorm)
                    ))
                }
                cancel()
            }
        }
    }

}