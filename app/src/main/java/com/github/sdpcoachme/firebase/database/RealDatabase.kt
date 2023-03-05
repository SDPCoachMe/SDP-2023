package com.github.sdpcoachme.firebase.database

class RealDatabase : Database {

    override fun isExistingUser(email: String): Boolean {
        return true
    }
}