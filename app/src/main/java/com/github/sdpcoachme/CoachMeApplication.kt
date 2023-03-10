package com.github.sdpcoachme

import android.app.Application
import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.auth.GoogleAuthenticator
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.RealDatabase

open class CoachMeApplication : Application() {
    // For DI in testing, add reference to dependencies here
    open val database: Database = RealDatabase()
    open val authenticator: Authenticator = GoogleAuthenticator()
}