package com.github.sdpcoachme

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.github.sdpcoachme.data.UserLocation
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.concurrent.CompletableFuture

/**
 * This class is used to launch the Places Autocomplete activity from anywhere in the app.
 *
 * @param context The context of the activity that is launching the Places Autocomplete activity.
 * @param caller The ActivityResultCaller that will be used to launch the Places Autocomplete activity.
 *
 */
class LocationAutocompleteHandler(context: Context, caller: ActivityResultCaller) {

    private lateinit var autocompleteResult: CompletableFuture<UserLocation>
    private val intent: Intent
    private val startForResult: ActivityResultLauncher<Intent>

    init {
        val fields = listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val filters = listOf(PlaceTypes.ADDRESS)
        intent = Autocomplete
            .IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .setTypesFilter(filters)
            .build(context)

        startForResult = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Handle the result from the Places Autocomplete activity that was started
            result: ActivityResult ->
                when (result.resultCode) {
                    Activity.RESULT_OK -> {
                        result.data!!.let {
                            val place = Autocomplete.getPlaceFromIntent(it)
                            autocompleteResult.complete(
                                UserLocation(
                                    // They should never be null anyways
                                    placeId = place.id!!,
                                    address = place.address!!,
                                    latitude = place.latLng!!.latitude,
                                    longitude = place.latLng!!.longitude
                                )
                            )
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        // The user canceled the operation
                        autocompleteResult.completeExceptionally(AutocompleteCancelledException())
                    }
                    else -> {
                        // There was an unknown error
                        // Log.d("AUTOCOMPLETE_STATUS", "Status ${Autocomplete.getStatusFromIntent(result.data!!)}") // access error details
                        autocompleteResult.completeExceptionally(AutocompleteFailedException())
                    }
                }
        }
    }

    /**
     * Launches the Places Autocomplete activity and returns a CompletableFuture that will be completed
     * with the UserLocation selected by the user.
     * Note this method can be called multiple times without issues, if for example the user cancels
     * the operation and wants to try again. The method returns a different CompletableFuture instance
     * each time it is called, and relaunches the Places Autocomplete activity.
     *
     * @return A CompletableFuture that will be completed with the UserLocation selected by the user.
     *        If the user cancels the operation, the CompletableFuture will fail with an
     *        AutocompleteCancelledException. If there is an error, the CompletableFuture will fail
     *        with an AutocompleteFailedException.
     */
    fun launch(): CompletableFuture<UserLocation> {
        autocompleteResult = CompletableFuture<UserLocation>()
        startForResult.launch(intent)
        return autocompleteResult
    }

    // Used to handle places autocomplete activity errors
    class AutocompleteFailedException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class AutocompleteCancelledException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
}