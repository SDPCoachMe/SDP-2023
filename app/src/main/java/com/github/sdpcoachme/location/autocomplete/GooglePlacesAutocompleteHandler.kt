package com.github.sdpcoachme.location.autocomplete

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.github.sdpcoachme.data.Address
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.concurrent.CompletableFuture

/**
 * This class is used to launch the Google Places Autocomplete activity from anywhere in the app.
 *
 * @param context The context of the activity that is launching the Places Autocomplete activity.
 * @param caller The ActivityResultCaller that will be used to launch the Places Autocomplete activity.
 *
 */
class GooglePlacesAutocompleteHandler(context: Context, caller: ActivityResultCaller) : AddressAutocompleteHandler {

    private lateinit var autocompleteResult: CompletableFuture<Address>
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
                                Address(
                                    // They should never be null anyways
                                    placeId = place.id!!,
                                    name = place.address!!,
                                    latitude = place.latLng!!.latitude,
                                    longitude = place.latLng!!.longitude
                                )
                            )
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        // The user canceled the operation
                        autocompleteResult.completeExceptionally(AddressAutocompleteHandler.AutocompleteCancelledException())
                    }
                    else -> {
                        // There was an unknown error
                        // Log.d("AUTOCOMPLETE_STATUS", "Status ${Autocomplete.getStatusFromIntent(result.data!!)}") // access error details
                        autocompleteResult.completeExceptionally(AddressAutocompleteHandler.AutocompleteFailedException())
                    }
                }
        }
    }

    /**
     * Launches the Places Autocomplete activity and returns a CompletableFuture that will be completed
     * with the UserAddress selected by the user.
     *
     * @return A CompletableFuture that will be completed with the UserAddress selected by the user.
     */
    override fun launch(): CompletableFuture<Address> {
        autocompleteResult = CompletableFuture<Address>()
        startForResult.launch(intent)
        return autocompleteResult
    }
}