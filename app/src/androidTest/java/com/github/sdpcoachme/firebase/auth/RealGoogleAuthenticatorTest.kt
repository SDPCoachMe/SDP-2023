package com.github.sdpcoachme.firebase.auth

import android.app.Activity
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class RealGoogleAuthenticatorTest {
    @Test
    fun onSignInWithFailedResultCodeExecutesFailCallBack() {
        val realGoogleAuthenticator = RealGoogleAuthenticator()
        val result = AtomicReference("")
        realGoogleAuthenticator.onSignInResult(
            FirebaseAuthUIAuthenticationResult(0, null),
            { result.set("success") },
            { result.set("failure") }
        )
        MatcherAssert.assertThat(result.get(), CoreMatchers.`is`("failure"))
    }

    @Test
    fun onSignInResultThrowsIfUserIsNull() {
        val realGoogleAuthenticator = RealGoogleAuthenticator()
        val error: Exception = Assert.assertThrows(
            IllegalStateException::class.java
        ) {
            realGoogleAuthenticator.onSignInResult(
                FirebaseAuthUIAuthenticationResult(Activity.RESULT_OK, null),
                {},
                {}
            )
        }
        MatcherAssert.assertThat(error.message, CoreMatchers.`is`("User is null"))
    }
}