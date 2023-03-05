package com.github.sdpcoachme.firebase.database

interface Database {
    fun isExestingUser(email: String): Boolean
}