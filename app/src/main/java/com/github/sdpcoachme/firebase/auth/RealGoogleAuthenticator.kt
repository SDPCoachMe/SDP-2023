package com.github.sdpcoachme.firebase.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import java.util.function.Consumer
import javax.inject.Inject

/**
 * Class that handles the Google sign in process
 */
class RealGoogleAuthenticator @Inject constructor() : GoogleAuthenticator {

    /**
     * Creates a sign in intent and launches it using the given launcher
     *
     * @param signInLauncher the launcher to use
     */
    fun createSignInIntent(signInLauncher: ActivityResultLauncher<Intent>) {
        // Choose authentication providers
        val providers = listOf(
            GoogleBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    override fun onSignInResult(
        result: FirebaseAuthUIAuthenticationResult?,
        onSuccess: Consumer<String?>?,
        onFailure: Runnable?
    ) {
        if (result!!.resultCode == Activity.RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User is null")
            onSuccess!!.accept(user.email)
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            onFailure!!.run()
        }
    }

    override fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        createSignInIntent(signInLauncher)
    }

    override fun delete(context: Context?, onComplete: Runnable?) {
        AuthUI.getInstance()
            .delete(context!!)
            .addOnCompleteListener { task: Task<Void?>? -> onComplete!!.run() }
    }

    override fun signOut(context: Context?, onComplete: Runnable?) {
        AuthUI.getInstance()
            .signOut(context!!)
            .addOnCompleteListener { task: Task<Void?>? -> onComplete!!.run() }
    }
}