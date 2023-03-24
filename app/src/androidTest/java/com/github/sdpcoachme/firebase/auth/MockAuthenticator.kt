package com.github.sdpcoachme.firebase.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import java.util.function.Consumer

class MockAuthenticator : Authenticator {
    private val realAuthenticator = GoogleAuthenticator()

    override fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        FirebaseAuth.getInstance().signInAnonymously()
    }

    override fun delete(context: Context?, onComplete: Runnable?) {
        onComplete?.run()
    }

    override fun signOut(context: Context?, onComplete: Runnable?) {
        onComplete?.run()
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