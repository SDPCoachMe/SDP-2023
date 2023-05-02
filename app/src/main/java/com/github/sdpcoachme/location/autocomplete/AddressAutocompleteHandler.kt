package com.github.sdpcoachme.location.autocomplete

import com.github.sdpcoachme.data.UserAddress
import java.util.concurrent.CompletableFuture

/**
 * This interface is used to launch the activity or process that allows the user to select an address.
 * The implementation should also handle the result of the activity or process and return a
 * UserAddress object.
 */
interface AddressAutocompleteHandler {

    /**
     * Launch the activity or process that allows the user to select an address.
     * Note this method can be called multiple times without issues, if for example the user cancels
     * the operation and wants to try again. The method should return a different CompletableFuture
     * instance each time it is called, and relaunches the process/activity necessary.
     *
     * @return A CompletableFuture that will be completed with the UserAddress selected by the user.
     * If the user cancels the operation, the CompletableFuture will fail with an AutocompleteCancelledException.
     * If there is an error, the CompletableFuture will fail with an AutocompleteFailedException.
     */
    fun launch(): CompletableFuture<UserAddress>

    // Used to handle places autocomplete activity errors
    class AutocompleteFailedException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class AutocompleteCancelledException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
}