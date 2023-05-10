package com.github.sdpcoachme.location.autocomplete

import com.github.sdpcoachme.data.UserAddress
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import java.util.concurrent.CompletableFuture

class MockAddressAutocompleteHandler : AddressAutocompleteHandler {
    companion object {
        val DEFAULT_ADDRESS = LAUSANNE
    }
    override fun launch(): CompletableFuture<UserAddress> {
        return CompletableFuture.completedFuture(DEFAULT_ADDRESS)
    }

}