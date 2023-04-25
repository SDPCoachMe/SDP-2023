package com.github.sdpcoachme.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import java.util.function.Consumer

/**
 * Interface responsible for the sign in process
 */
interface Authenticator {
    /**
     * Creates a sign in intent and launches it using the given launcher
     *
     * @param signInLauncher the launcher to use
     */
    fun signIn(signInLauncher: ActivityResultLauncher<Intent>)

    /**
     * Deletes the current user
     *
     * @param context the context to use
     * @param onComplete the callback to call on completion
     */
    fun delete(context: Context?, onComplete: Runnable?)

    /**
     * Signs out the current user
     *
     * @param context the context to use
     * @param onComplete the callback to call on completion
     */
    fun signOut(context: Context?, onComplete: Runnable?)

    /**
     * Handles the result of the sign in intent and calls the appropriate callback based on success or failure
     *
     * @param result the result of the sign in intent
     * @param onSuccess the callback to call on success
     * @param onFailure the callback to call on failure
     */
    fun onSignInResult(
        result: FirebaseAuthUIAuthenticationResult?,
        onSuccess: Consumer<String?>?,
        onFailure: Consumer<String?>?
    )

    /**
     * Checks if the user is signed in
     *
     * @return true if the user is signed in, false otherwise
     */
    fun isSignedIn(): Boolean
}