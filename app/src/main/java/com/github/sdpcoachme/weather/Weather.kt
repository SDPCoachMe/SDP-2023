package com.github.sdpcoachme.weather

/**
 * Represents the weather sufficient info to be displayed.
 *
 * @param weatherCode weather icon id !!
 * @param maxTemperature maximal apparent temperature of the day
 * @param minTemperature minimal apparent temperature of the day
 */
data class Weather(
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double
)

/**
 * Serializable list of Weather. Especially useful for CachingStore.
 */
data class WeatherForecast(
    val forecast: List<Weather>
) {
    // Constructor needed to make the data class serializable
    constructor() : this(emptyList())
}

