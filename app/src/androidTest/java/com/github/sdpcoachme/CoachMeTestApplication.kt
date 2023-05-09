package com.github.sdpcoachme

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.MockAuthenticator
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.autocomplete.LocationAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.MockLocationAutocompleteHandler

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    // todo for emulator testing
    //override var database: Database = FireDatabase(Firebase.database.reference)

    private val TEST_PREFERENCES_NAME = "coachme_test_preferences"



    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = TEST_PREFERENCES_NAME)
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
        store = CachingStore(MockDatabase(), dataStore, this)

        // Might be necessary to initialize Places SDK, but for now, we don't need it.
    }
    override val authenticator: Authenticator = MockAuthenticator()

    override fun onTerminate() {
        super.superOnTerminate()
    }

    /**
     * Clear the data store and reset the caching store.
     */
    fun clearDataStoreAndResetCachingStore() {
        store = CachingStore(MockDatabase(), dataStore, this)
    }

    override fun locationAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): LocationAutocompleteHandler = MockLocationAutocompleteHandler()
}