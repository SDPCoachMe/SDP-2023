package com.github.sdpcoachme.firebase.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import java.util.function.Consumer

class MockGoogleAuthenticator : GoogleAuthenticator {
    override fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        RealGoogleAuthenticator().signIn(signInLauncher)
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
        println("MockGoogleAuthenticator.onSignInResult()")
    }
}