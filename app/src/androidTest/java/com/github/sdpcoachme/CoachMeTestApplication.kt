package com.github.sdpcoachme

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.MockAuthenticator
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.autocomplete.AddressAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.MockAddressAutocompleteHandler

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

        // Might be necessary to initialize Places SDK, but for now, we don't need it.
    }
    override val authenticator: Authenticator = MockAuthenticator()

    override fun addressAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): AddressAutocompleteHandler = MockAddressAutocompleteHandler()
}