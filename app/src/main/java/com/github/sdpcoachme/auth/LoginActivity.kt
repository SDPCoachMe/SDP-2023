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
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.LOG_IN
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.messaging.InAppNotificationService
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture


class LoginActivity : ComponentActivity() {

    // Used to make sure the activity does not display its content if it will be redirected
    private var displayUI: CompletableFuture<Boolean> = CompletableFuture()

    // Allows to notice testing framework that the activity is ready
    var stateLoading: CompletableFuture<Void> = displayUI.thenAccept {} // To have Void type

    class TestTags {
        class Buttons {
            companion object {
                const val LOG_IN = "logInButton"
            }
        }
    }

    private lateinit var store : CachingStore
    private lateinit var authenticator: Authenticator
    // Register the launcher to handle the result of Google Authentication
    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult (
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        authenticator.onSignInResult(
            res,
            onSuccess = { email ->
                // Should not be null on success
                store.setCurrentEmail(email!!).thenAccept {
                    // Once we are logged in, we can add the FCM token to the database.
                    // Note on FCM tokens: only the newest device on which the user logged in will
                    // have the FCM token and receive notifications. This is done for simplicity as
                    // otherwise, we would need to remove notifications on one device when they are
                    // read on another. This could be added/handled in a future sprint.
                    InAppNotificationService.getFCMToken().thenCompose {
                        store.setFCMToken(email, it)
                    }.thenAccept {
                        launchNextActivity(email)
                    }
                }
            },
            onFailure = {
                val errorMsg = "There was an error while trying to log in."
                ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
            }
        )
    }
    // Register the launcher to ask for notification permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    // Allows to use testTagsAsResourceId feature (since tests are done with UI Automator, it is
    // necessary to map test tags to resource IDs)
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticator = (application as CoachMeApplication).authenticator
        store =  (application as CoachMeApplication).store

        store.isLoggedIn().thenAccept { loggedIn ->
            if (loggedIn) {
                store.getCurrentEmail().thenAccept {
                    launchNextActivity(it)
                    // Notify tests
                    displayUI.complete(false)
                }
            } else {
                // If the user is not logged in, we can start loading the activity
                // Also notify tests
                displayUI.complete(true)
            }
        }

        setContent {
            CoachMeTheme {
                Surface(
                    color = MaterialTheme.colors.background,
                ) {
                    Scaffold(
                        // Enables for all composables in the hierarchy.
                        modifier = Modifier.semantics {
                            testTagsAsResourceId = true
                        }
                    ) {
                        // Need to pass padding to child node
                            innerPadding ->
                        AuthenticationForm(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    @Composable
    fun AuthenticationForm(modifier: Modifier = Modifier) {

        var showUI by remember { mutableStateOf(false) }

        LaunchedEffect(true) {
            showUI = displayUI.await()
        }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showUI) {
                // TODO: add app icon and make this activity look nicer.
                Button(
                    modifier = Modifier.testTag(LOG_IN),
                    onClick = {
                        authenticator.signIn(signInLauncher)
                    })
                { Text("LOG IN") }
            } else {
                // While we are waiting for the next activity to launch, we display a loading
                CircularProgressIndicator()
            }
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

    private fun launchNextActivity(email: String) {
        store.userExists(email).thenAccept { exists ->
            val intent = if (!exists) {
                // If the user does not exist, launch sign up activity
                Intent(this, SignupActivity::class.java)
            } else {
                // Otherwise, decide where to redirect
                // Check if a notification was clicked
                val pushNotificationIntent = intent
                val action = pushNotificationIntent.action
                if (action.equals("OPEN_CHAT_ACTIVITY")
                    && pushNotificationIntent.getStringExtra("chatId") != null) {
                    // If a notification was clicked, redirect to chat activity
                    Intent(this, CoachesListActivity::class.java)
                        .putExtra("openChat", true)
                        .putExtra("chatId", pushNotificationIntent.getStringExtra("chatId"))
                        .putExtra("pushNotification_currentUserEmail", email)
                } else {
                    // If no notification was clicked, redirect to map activity
                    Intent(this, MapActivity::class.java)
                }
            }

            // Launch the next activity
            startActivity(intent)
            // TODO: notification should be asked in Map Activity, not here. Keep it like this for
            //  now.
            askNotificationPermission()
        }
    }
}