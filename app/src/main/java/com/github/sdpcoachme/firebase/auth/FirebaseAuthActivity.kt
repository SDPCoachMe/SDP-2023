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
import androidx.compose.ui.res.stringResource
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import javax.inject.Inject


class FirebaseAuthActivity : ComponentActivity() {
    private var signInInfo: String by mutableStateOf("Not signed in")

//    @Inject
    private lateinit var googleAuthenticator: GoogleAuthenticator


    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        googleAuthenticator.onSignInResult(
            res,
            { email -> signInInfo = String.format(getString(R.string.signed_in_as), email) },
            { errorMsg -> signInInfo = errorMsg.toString() }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuthenticator = (application as CoachMeApplication).googleAuthenticator

        setContent {
            CoachMeTheme {
                AuthenticationForm(
                    signInInfo = this.signInInfo
                )
            }
        }

//        DaggerGoogleAuthComponent.builder()
//            .googleAuthModule(GoogleAuthModule())
//            .build()
//            .inject(this)
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
//    var signInInfo by remember { mutableStateOf("Please log in to your Google account") }
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