package com.github.sdpcoachme

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.github.sdpcoachme.data.messaging.FCMToken
import com.github.sdpcoachme.firebase.auth.Authenticator
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.map.MapActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging


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

    private fun addFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            println("FCM registration Token: $token")

            // TODO: check if token can be added to DB
            // Faster to always set than always get and sometimes also set...
            database.setFCMToken(database.getCurrentEmail(), FCMToken(token!!, true))
        })
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                println("FCM SDK (and your app) can post notifications.")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                println("in the else if of askNotificationPermission")
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                println("in the else of askNotificationPermission")
            }
        } else {
            println("FCM SDK (and your app) can post notifications as api level < 33.")
            // TODO: still create request for permission...
        }
    }

    private fun launchPostLoginActivity(email: String) {
        database.userExists(email).thenAccept { exists ->
            if (exists) {
                // check if inside the db...
                addFCMTokenToDatabase() //TODO: check if this is the right place to add the token to the DB (and add the check for the adding to db...


                launchActivity(MapActivity::class.java)
            } else {
                //TODO: as user for permission for the fcm token push notifications...


                launchActivity(SignupActivity::class.java)
            }
        }

    }

    private fun launchActivity(activity: Class<*>) {
        val intent = Intent(this, activity)
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