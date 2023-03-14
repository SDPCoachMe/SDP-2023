package com.github.sdpcoachme

import android.app.Application
import com.google.firebase.FirebaseApp


open class CoachMeApplication : Application() {
    // For DI in testing, add reference to dependencies here
    open lateinit var database: Database
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        database = FireDatabase()
    }

}