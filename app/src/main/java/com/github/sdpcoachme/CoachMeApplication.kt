package com.github.sdpcoachme

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
//import androidx.datastore.core.DataStore
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.GoogleAuthenticator
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.FireDatabase
import com.github.sdpcoachme.location.autocomplete.GooglePlacesAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.LocationAutocompleteHandler
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.*

open class CoachMeApplication : Application() {

    private val USER_PREFERENCES_NAME = "coachme_preferences"

    //private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

    // For DI in testing, add reference to dependencies here
    open lateinit var store: CachingStore
    // We don't want to put the exact user location in the database, so it will
    // be stored here, therefore similarly to the database
    open lateinit var userLocation: MutableState<LatLng?>


    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        store = CachingStore(FireDatabase(Firebase.database.reference),
            //dataStore,
            this)
        userLocation = mutableStateOf(null)

        // Initialize Places SDK
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }

    /*
    override fun onTerminate() {
        super.onTerminate()
        store.storeLocalData()
    }

    fun superOnTerminate() {
        super.onTerminate()
    }

     */

    open val authenticator: Authenticator = GoogleAuthenticator()

    open fun locationAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): LocationAutocompleteHandler = GooglePlacesAutocompleteHandler(context, caller)
}