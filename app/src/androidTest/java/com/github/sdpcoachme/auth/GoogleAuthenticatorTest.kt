package com.github.sdpcoachme.auth

import android.app.Activity
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.ui.auth.data.model.User
import com.github.sdpcoachme.auth.GoogleAuthenticator
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class GoogleAuthenticatorTest {
    
    @Test
    fun onSignInResultCallsOnFailureIfResultIsNull() {
        val realGoogleAuthenticator = GoogleAuthenticator()
        var onFailureMsg = ""
        realGoogleAuthenticator.onSignInResult(
            null,
            {onFailureMsg = "success"},
            {errorMsg -> onFailureMsg = errorMsg?:"errorMsg is null"}
        )
        MatcherAssert.assertThat(onFailureMsg, `is`("login error"))
    } 
    @Test
    fun onSignInWithFailedResultCodeExecutesFailCallBack() {
        val realGoogleAuthenticator = GoogleAuthenticator()
        val result = AtomicReference("")
        realGoogleAuthenticator.onSignInResult(
            FirebaseAuthUIAuthenticationResult(0, null),
            { result.set("success") },
            { result.set("failure") }
        )
        MatcherAssert.assertThat(result.get(), `is`("failure"))
    }

    @Test
    fun onSignInResultThrowsIfUserIsNull() {
        FirebaseAuth.getInstance().signOut()
        val realGoogleAuthenticator = GoogleAuthenticator()
        var message = ""
        val error: Exception = Assert.assertThrows(
            IllegalStateException::class.java
        ) {
            realGoogleAuthenticator.onSignInResult(
                FirebaseAuthUIAuthenticationResult(Activity.RESULT_OK, null),
                {message = "success"},
                {message = "failure"}
            )
        }
        MatcherAssert.assertThat(error.message, `is`("User is null"))
        println("message: $message")
    }

    @Test
    fun onSignResultCallsOnFailureWhenUserCancelsSignIn() {
        val realGoogleAuthenticator = GoogleAuthenticator()

        var onFailureMsg = ""

        realGoogleAuthenticator.onSignInResult(
            FirebaseAuthUIAuthenticationResult(Activity.RESULT_CANCELED, null),
            {onFailureMsg = "success"},
            {errorMsg -> onFailureMsg = errorMsg?:"errorMsg is null"}
        )

        MatcherAssert.assertThat(onFailureMsg, `is`("User cancelled sign in"))
    }

    @Test
    fun onSignInResultCallsOnFailureWhenResultCodeIsNotOkOrCanceled() {

        val response = IdpResponse.Builder(
            User.Builder("provider", "email").build())
            .setNewUser(false)
            .setSecret("secret")
            .setToken("token")
            .setPendingCredential(null)
            .build()

        val realGoogleAuthenticator = GoogleAuthenticator()

        var onFailureMsg = ""

        realGoogleAuthenticator.onSignInResult(
            FirebaseAuthUIAuthenticationResult(1, response),
            {onFailureMsg = "success"},
            {errorMsg -> onFailureMsg = errorMsg ?: "errorMsg is null"}
        )

        MatcherAssert.assertThat(onFailureMsg, `is`("login error: null"))
    }

    @Test
    fun isSignedInReturnsFalseIfUserNotSignedIn() {
        FirebaseAuth.getInstance().signOut()
        assertThat(GoogleAuthenticator().isSignedIn(), `is`(false))
    }
}