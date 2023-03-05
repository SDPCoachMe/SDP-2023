package com.github.sdpcoachme.firebase.database

interface Database {
    fun isExistingUser(email: String): Boolean
}