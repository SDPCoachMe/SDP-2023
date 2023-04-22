package com.github.sdpcoachme.data.messaging

data class FCMToken(
    val token: String,
    val permissionGranted: Boolean,
) {
    constructor() : this("", true)
}