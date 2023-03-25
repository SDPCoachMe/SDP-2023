package com.github.sdpcoachme

import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.auth.MockAuthenticator
import com.github.sdpcoachme.firebase.database.FireDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    override fun onCreate() {
        super.onCreate()
        // 10.0.2.2 is the special IP address to connect to the 'localhost' of
        // the host computer from an Android emulator.
        try {
            Firebase.database.useEmulator("10.0.2.2", 9000)
            database = FireDatabase(Firebase.database.reference)
        } catch (e: Exception) {
            // Ignore
        }
    }
    override val authenticator: Authenticator = MockAuthenticator()
}