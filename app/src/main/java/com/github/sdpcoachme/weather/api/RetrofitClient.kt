package com.github.sdpcoachme.weather.api

import com.github.sdpcoachme.weather.repository.OpenMeteoRepository.Companion.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Client to instantiate a weather API using the retrofit framework.
 * Here the chosen api is open-meteo.com.
 */
object RetrofitClient {

    private val retrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WeatherApi by lazy {
        retrofit.create(WeatherApi::class.java)
    }

}