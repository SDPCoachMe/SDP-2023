package com.github.sdpcoachme.data.messaging

data class FCMToken(
    val token: String = "",
    val permissionGranted: Boolean = false,
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", false)
}