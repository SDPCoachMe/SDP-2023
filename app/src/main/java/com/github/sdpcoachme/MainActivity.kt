package com.github.sdpcoachme

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class MainActivity : ComponentActivity() {

    private lateinit var database : Database
    private var signInInfo: String by mutableStateOf("Not signed in")
    private lateinit var authenticator: Authenticator
    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult (
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        authenticator.onSignInResult(
            res,
            { email -> launchPostLoginActivity(email ?: "") },
            { errorMsg -> signInInfo = errorMsg.toString() }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticator = (application as CoachMeApplication).authenticator
        this.database =  (application as CoachMeApplication).database

        if (authenticator.isSignedIn()) {
            this.signIntoAccount()
        } else {
            setContent {
                CoachMeTheme {
                    AuthenticationForm(
                        signInInfo = this.signInInfo,
                        context = this
                    )
                }
            }
        }
    }

    private fun launchPostLoginActivity(email : String) {
        val intent = if (database.isExistingUser(email)) {
            Intent(this, DashboardActivity::class.java)
        } else {
            Intent(this, SignUpActivity::class.java)
        }

        intent.putExtra("email", email)
        startActivity(intent)
    }

    /**
     * Deletes the google account from the device
     */
    fun deleteAccount() {
        authenticator.delete(this) { signInInfo = getString(R.string.account_deleted) }
    }

    /**
     * Signs into the google account
     */
    fun signIntoAccount() {
        authenticator.signIn(signInLauncher)
    }

    /**
     * Signs out of the google account
     */
    fun signOutOfAccount() {
        authenticator.signOut(this) { signInInfo = getString(R.string.signed_out) }
    }
}

@Composable
fun AuthenticationForm(signInInfo: String, context: MainActivity) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.testTag("sign_in_info"),
            text = signInInfo,
        )

        Button(
            modifier = Modifier.testTag("sign_in_button"),
            onClick = {
                context.signIntoAccount()
            })
        { Text(stringResource(id = R.string.sign_in_button_text)) }
        Button(
            modifier = Modifier.testTag("sign_out_button"),
            onClick = {
                context.signOutOfAccount()
            })
        { Text(stringResource(id = R.string.sign_out_button_text)) }
        Button(
            modifier = Modifier.testTag("delete_button"),
            onClick = {
                context.deleteAccount()
            })
        { Text(stringResource(id = R.string.delete_account_button_text)) }
    }
}