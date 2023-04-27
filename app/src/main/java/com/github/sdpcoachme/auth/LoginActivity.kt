package com.github.sdpcoachme.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.messaging.InAppNotificationService.Companion.addFCMTokenToDatabase
import com.github.sdpcoachme.ui.theme.CoachMeTheme


class LoginActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val INFO_TEXT = "signInInfo"
        }
        class Buttons {
            companion object {
                const val SIGN_IN = "signInButton"
                const val SIGN_OUT = "signOutButton"
                const val DELETE_ACCOUNT = "deleteAccountButton"
            }
        }
    }

    private lateinit var userExistsIntent: Intent
    private lateinit var database : Database
    private var signInInfo: String by mutableStateOf("Not signed in")
    private lateinit var authenticator: Authenticator
    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult (
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        authenticator.onSignInResult(
            res,
            { email ->
                // Should not be null on success
                email!!.let {
                    signInInfo = email
                    database.setCurrentEmail(it)
                    launchPostLoginActivity(it)
                }
            },
            { errorMsg -> signInInfo = errorMsg.toString() }
        )
    }

    // Allows to use testTagsAsResourceId feature (since tests are done with UI Automator, it is
    // necessary to map test tags to resource IDs)
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticator = (application as CoachMeApplication).authenticator
        database =  (application as CoachMeApplication).database

        // Check if a notification was clicked
        val pushNotificationIntent = intent
        val action = pushNotificationIntent.action
        userExistsIntent = Intent(this, MapActivity::class.java)
        if (action.equals("OPEN_CHAT_ACTIVITY") && pushNotificationIntent.getStringExtra("sender") != null) {
            userExistsIntent = Intent(this, ChatActivity::class.java)
                        .putExtra("toUserEmail", pushNotificationIntent.getStringExtra("sender"))

            if (database.getCurrentEmail().isNotEmpty()) {
                startActivity(userExistsIntent)
            }
        }


        setContent {
            CoachMeTheme {
                Scaffold(
                    // Enables for all composables in the hierarchy.
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    }
                ) {
                    // Need to pass padding to child node
                    innerPadding ->
                        AuthenticationForm(
                            signInInfo = this.signInInfo,
                            context = this,
                            modifier = Modifier.padding(innerPadding)
                        )
                }
            }
        }
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Asks the user for permission to post notifications.
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // As discussed on discord with Léo and Kamilla,
            // for API level < 33, notifications cannot be enabled / disabled in the app
            // and the user needs to disable them in the settings.
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            // Notifications have already been enabled, no need to ask again.
            return
        }

        // Request permission to post notifications.
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun launchPostLoginActivity(email: String) {
        database.userExists(email).thenAccept { exists ->
            if (exists) {
                startActivity(userExistsIntent)
            } else {
                startActivity(Intent(this, SignupActivity::class.java))
            }

            askNotificationPermission()
            addFCMTokenToDatabase(database)
        }
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
fun AuthenticationForm(signInInfo: String, context: LoginActivity, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.testTag(LoginActivity.TestTags.INFO_TEXT),
            text = signInInfo,
        )

        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.SIGN_IN),
            onClick = {
                context.signIntoAccount()
            })
        { Text(stringResource(id = R.string.sign_in_button_text)) }
        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.SIGN_OUT),
            onClick = {
                context.signOutOfAccount()
            })
        { Text(stringResource(id = R.string.sign_out_button_text)) }
        Button(
            modifier = Modifier.testTag(LoginActivity.TestTags.Buttons.DELETE_ACCOUNT),
            onClick = {
                context.deleteAccount()
            })
        { Text(stringResource(id = R.string.delete_account_button_text)) }
    }
}