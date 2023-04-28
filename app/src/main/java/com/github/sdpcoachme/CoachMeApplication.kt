package com.github.sdpcoachme

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.auth.Authenticator
import com.github.sdpcoachme.auth.GoogleAuthenticator
import com.github.sdpcoachme.database.CachingDatabase
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.FireDatabase
import com.github.sdpcoachme.location.autocomplete.GooglePlacesAutocompleteHandler
import com.github.sdpcoachme.location.autocomplete.AddressAutocompleteHandler
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

open class CoachMeApplication : Application() {
    // For DI in testing, add reference to dependencies here
    open lateinit var database: Database
    // We don't want to put the exact user location in the database, so it will
    // be stored here, therefore similarly to the database
    open lateinit var userLocation: MutableState<LatLng?>

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        database = CachingDatabase(FireDatabase(Firebase.database.reference))
        userLocation = mutableStateOf(null)

        // Initialize Places SDK
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }

    open val authenticator: Authenticator = GoogleAuthenticator()

    open fun addressAutocompleteHandler(
        context: Context,
        caller: ActivityResultCaller
    ): AddressAutocompleteHandler = GooglePlacesAutocompleteHandler(context, caller)
}