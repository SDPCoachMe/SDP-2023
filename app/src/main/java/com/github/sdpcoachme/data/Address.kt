package com.github.sdpcoachme.data

data class Address (
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
    // Latitude, longitude and address should not be cached for more than 30 days according to Google,
    // but this is a prototype and we can't afford requesting Google Places API all the time to refresh
    // the data.
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", 0.0, 0.0)
}