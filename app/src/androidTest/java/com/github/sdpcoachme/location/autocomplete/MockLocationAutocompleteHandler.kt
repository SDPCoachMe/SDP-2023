package com.github.sdpcoachme.location.autocomplete

import com.github.sdpcoachme.data.UserLocation
import com.github.sdpcoachme.location.UserLocationSamples.Companion.LAUSANNE
import java.util.concurrent.CompletableFuture

class MockLocationAutocompleteHandler : LocationAutocompleteHandler {
    companion object {
        val DEFAULT_LOCATION = LAUSANNE
    }
    override fun launch(): CompletableFuture<UserLocation> {
        return CompletableFuture.completedFuture(DEFAULT_LOCATION)
    }

}