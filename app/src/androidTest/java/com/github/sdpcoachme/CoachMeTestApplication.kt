package com.github.sdpcoachme

import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.auth.MockAuthenticator
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.MockDatabase
import com.google.android.libraries.places.api.Places

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    // todo for emulator testing
    //override var database: Database = FireDatabase(Firebase.database.reference)
    override var database: Database = MockDatabase()
    override fun onCreate() {
        super.onCreate()
        // 10.0.2.2 is the special IP address to connect to the 'localhost' of
        // the host computer from an Android emulator.
        // todo for emulator testing
        /*
        try {
            val db = Firebase.database
            db.useEmulator("10.0.2.2", 9000)
            database = FireDatabase(db.reference)
        } catch (e: Exception) {
            // Ignore
        }
         */
        database = MockDatabase()

        // Initialize Places SDK (mocking would be way too complex here)
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }
    override val authenticator: Authenticator = MockAuthenticator()
}