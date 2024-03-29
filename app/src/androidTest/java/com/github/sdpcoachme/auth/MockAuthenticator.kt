package com.github.sdpcoachme.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.github.sdpcoachme.location.MapActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.function.Consumer

class MockAuthenticator : Authenticator {
    private val realAuthenticator = GoogleAuthenticator()

    override fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        FirebaseAuth.getInstance().signInAnonymously()
    }

    override fun delete(context: Context?, onComplete: Runnable?) {
        realAuthenticator.delete(context, onComplete)
    }

    override fun signOut(context: Context?, onComplete: Runnable?) {
        // Done since the tests for map activity have no timeout
        // capabilities to wait for the asynchronous sign-out to finish
        // and, therefore, sometimes would fail
        if (context is MapActivity) {
            onComplete?.run()
        } else {
            realAuthenticator.signOut(context, onComplete)
        }
    }

    override fun onSignInResult(
        result: FirebaseAuthUIAuthenticationResult?,
        onSuccess: Consumer<String?>?,
        onFailure: Consumer<String?>?
    ) {
        realAuthenticator.onSignInResult(result, onSuccess, onFailure)
    }

    override fun isSignedIn(): Boolean {
        return false
    }
}