package com.github.sdpcoachme

import android.app.Application
import com.github.sdpcoachme.firebase.auth.GoogleAuthenticator
import com.github.sdpcoachme.firebase.auth.RealGoogleAuthenticator

open class CoachMeApplication : Application() {
    // For DI in testing, add reference to dependencies here
    open val googleAuthenticator: GoogleAuthenticator = RealGoogleAuthenticator()
}