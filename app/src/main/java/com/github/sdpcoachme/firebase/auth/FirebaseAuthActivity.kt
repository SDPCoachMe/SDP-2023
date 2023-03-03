package com.github.sdpcoachme.firebase.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.R
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import javax.inject.Inject


class FirebaseAuthActivity() : ComponentActivity() {

    @Inject
    lateinit var googleAuthenticator: GoogleAuthenticator

    private var signInInfo: String by mutableStateOf("Not signed in")

    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        googleAuthenticator.onSignInResult(
            res, { email ->
                signInInfo =
                    String.format(getString(R.string.signed_in_as), email)
            }
        ) {
            signInInfo = getString(R.string.sign_in_failed)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CoachMeTheme {
                AuthenticationForm(
                    signInInfo = this.signInInfo
                )
            }
        }
//        setContentView(R.layout.activity_firebase_ui)

        DaggerGoogleAuthComponent.builder()
            .googleAuthModule(GoogleAuthModule())
            .build()
            .inject(this)
    }

    /**
     * Deletes the google account from the device
     */
    fun deleteGoogleAccount() {
        googleAuthenticator.delete(this) { signInInfo = getString(R.string.deleted_accout) }
    }

    /**
     * Signs into the google account
     */
    fun signIntoGoogleAccount() {
        googleAuthenticator.signIn(signInLauncher)
    }

    /**
     * Signs out of the google account
     */
    fun signOutOfGoogleAccount() {
        googleAuthenticator.signOut(this) { signInInfo = getString(R.string.signed_out) }
    }
}

@Composable
fun AuthenticationForm(signInInfo: String) {
//    var signInInfo by remember { mutableStateOf("Please log in to your Google account") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            modifier = Modifier.testTag("sign_in_info"),
            value = signInInfo,
            onValueChange = {},
            label = { Text("Current Account Status") }
        )

        Button(
            modifier = Modifier.testTag("sign_in_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.signIntoGoogleAccount()
            })
        { Text("Sign in") }
        Button(
            modifier = Modifier.testTag("sign_out_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.signOutOfGoogleAccount()
            })
        { Text("Sign out") }
        Button(
            modifier = Modifier.testTag("delete_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.deleteGoogleAccount()
            })
        { Text("Delete") }
    }
}