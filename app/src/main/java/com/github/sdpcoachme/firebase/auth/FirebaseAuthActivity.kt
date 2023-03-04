package com.github.sdpcoachme.firebase.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.R
import com.github.sdpcoachme.ui.theme.CoachMeTheme


class FirebaseAuthActivity : ComponentActivity() {
    private var signInInfo: String by mutableStateOf("Not signed in")

    private lateinit var googleAuthenticator: GoogleAuthenticator

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuthenticator = RealGoogleAuthenticator()

        signInLauncher = registerForActivityResult (
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            googleAuthenticator.onSignInResult(
                res,
                { email -> signInInfo = String.format(getString(R.string.signed_in_as), email) },
                { errorMsg -> signInInfo = errorMsg.toString() }
            )
        }

        setContent {
            CoachMeTheme {
                AuthenticationForm(
                    signInInfo = this.signInInfo
                )
            }
        }
    }

    /**
     * Deletes the google account from the device
     */
    fun deleteGoogleAccount() {
        googleAuthenticator.delete(this) { signInInfo = getString(R.string.account_deleted) }
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
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // read only text field

        Text(
            modifier = Modifier.testTag("sign_in_info"),
            text = signInInfo,
        )

        Button(
            modifier = Modifier.testTag("sign_in_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.signIntoGoogleAccount()
            })
        { Text(stringResource(id = R.string.sign_in_button_text)) }
        Button(
            modifier = Modifier.testTag("sign_out_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.signOutOfGoogleAccount()
            })
        { Text(stringResource(id = R.string.sign_out_button_text)) }
        Button(
            modifier = Modifier.testTag("delete_button"),
            onClick = {
                (context as? FirebaseAuthActivity)?.deleteGoogleAccount()
            })
        { Text(stringResource(id = R.string.delete_account_button_text)) }
    }
}