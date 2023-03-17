package com.github.sdpcoachme.firebase.database

class NoSuchKeyException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}