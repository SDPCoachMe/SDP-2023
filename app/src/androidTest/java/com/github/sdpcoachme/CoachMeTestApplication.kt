package com.github.sdpcoachme

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.MockAuthenticator
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.autocomplete.AddressAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.MockAddressAutocompleteHandler
import com.github.sdpcoachme.location.provider.MockLocationProvider
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coachme_test_preferences")

class CoachMeTestApplication : CoachMeApplication() {
    // For DI in testing, add reference to mocks here
    // todo for emulator testing
    //override var database: Database = FireDatabase(Firebase.database.reference)

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
        locationProvider = MockLocationProvider()
        // Might be necessary to initialize Places SDK, but for now, we don't need it.
    }
    override val authenticator: Authenticator = MockAuthenticator()

    /**
     * Clear the data store and reset the caching store.
     */
    fun clearDataStoreAndResetCachingStore() {
        runBlocking {
            dataStore.edit { it.clear() }
        }
        store = CachingStore(MockDatabase(), dataStore, this)
    }

    override fun addressAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): AddressAutocompleteHandler = MockAddressAutocompleteHandler()
}