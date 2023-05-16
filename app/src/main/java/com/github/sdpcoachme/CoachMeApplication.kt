package com.github.sdpcoachme

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.datastore.core.DataStore
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.GoogleAuthenticator
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.FireDatabase
import com.github.sdpcoachme.location.autocomplete.AddressAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.GooglePlacesAutocompleteHandler
import com.github.sdpcoachme.location.provider.FusedLocationProvider
import com.github.sdpcoachme.location.provider.LocationProvider
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.*

open class CoachMeApplication : Application() {

    private val USER_PREFERENCES_NAME = "coachme_preferences"

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

    // For DI in testing, add reference to dependencies here
    open lateinit var store: CachingStore
    // We don't want to put the exact user location in the database, so it will
    // be stored here thanks to a locationProvider that will handle all location-related processes
    open lateinit var locationProvider: LocationProvider

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        store = CachingStore(FireDatabase(Firebase.database.reference), dataStore, this)
        locationProvider = FusedLocationProvider()

        // Initialize Places SDK
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }

    open val authenticator: Authenticator = GoogleAuthenticator()

    open fun addressAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): AddressAutocompleteHandler = GooglePlacesAutocompleteHandler(context, caller)
}