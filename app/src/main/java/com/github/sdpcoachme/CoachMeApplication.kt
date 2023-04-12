package com.github.sdpcoachme

import android.app.Application
import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.auth.GoogleAuthenticator
import com.github.sdpcoachme.firebase.database.CachingDatabase
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.FireDatabase
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


open class CoachMeApplication : Application() {
    // For DI in testing, add reference to dependencies here
    open lateinit var database: Database
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        database = CachingDatabase(FireDatabase(Firebase.database.reference))

        // Initialize Places SDK
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }

    open val authenticator: Authenticator = GoogleAuthenticator()
}