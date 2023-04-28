package com.github.sdpcoachme.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocation
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.location.autocomplete.LocationAutocompleteHandler
import com.github.sdpcoachme.profile.SelectSportsActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class SignupActivity : ComponentActivity() {

    // Used to notify testing framework that the activity has finished sending data to the database
    val databaseStateSending = CompletableFuture<Void>()
    class TestTags {
        class TextFields {
            companion object {
                const val FIRST_NAME = "firstName"
                const val LAST_NAME = "lastName"
                const val PHONE = "phone"
            }
        }
        class Buttons {
            companion object {
                const val SIGN_UP = "signUpButton"
                const val BE_COACH = "BE_COACH_SWITCH"
            }
        }
    }

    private lateinit var database : Database
    private lateinit var email: String
    private lateinit var locationAutocompleteHandler: LocationAutocompleteHandler
    private lateinit var selectSportsHandler: (Intent) -> CompletableFuture<List<Sports>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()

        if (email.isEmpty()) {
            val errorMsg = "The signup page did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            // Set up handler for calls to location autocomplete
            locationAutocompleteHandler = (application as CoachMeApplication).locationAutocompleteHandler(this, this)

            // Set up handler for calls to select sports
            selectSportsHandler = SelectSportsActivity.getHandler(this)

            setContent {
                CoachMeTheme {
                    AccountForm(email)
                }
            }
        }
    }

    @Composable
    fun AccountForm(email: String) {
        val context = LocalContext.current

        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var isCoach by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.FIRST_NAME),
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.LAST_NAME),
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.PHONE),
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.clearFocus() }
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("I would like to be a coach")
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = isCoach,
                    onCheckedChange = { isCoach = it },
                    modifier = Modifier.testTag(TestTags.Buttons.BE_COACH)
                )
            }
            Button(
                modifier = Modifier.testTag(TestTags.Buttons.SIGN_UP),
                onClick = {
                    var newUser = UserInfo(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        coach = isCoach,
                        // location added later with LocationAutocompleteHandler
                        location = UserLocation(),
                        // sports added later in SelectSportsActivity
                        sports = listOf()
                    )
                    // Launch autocomplete activity, then wait for result
                    locationAutocompleteHandler.launch().thenCompose { location ->
                        newUser = newUser.copy(location = location)
                        // Update database
                        // Note: the reason we update the database here already is for compatibility
                        // with the test framework. Obscure issues appear if we allow the signup
                        // activity to launch the map activity in the tests. This means that we have
                        // to test that the database is updated correctly here, instead of after the
                        // map activity is launched.
                        database.updateUser(newUser)
                    }.thenCompose {
                        // Notify test framework that the activity has finished sending data to the database
                        databaseStateSending.complete(null)
                        // Launch SelectSportsActivity
                        selectSportsHandler(
                            SelectSportsActivity.getIntent(
                                context = context,
                                initialValue = emptyList(),
                                title = "Select favorite sports"
                            )
                        )
                    }.thenCompose { sports ->
                        newUser = newUser.copy(sports = sports)
                        // Update database with sports (this one is not tested)
                        database.updateUser(newUser)
                    }.thenAccept {
                        // Go to main activity
                        startActivity(Intent(context, MapActivity::class.java))
                    }.exceptionally {
                        when (it.cause) {
                            is LocationAutocompleteHandler.AutocompleteCancelledException -> {
                                // The user cancelled the Places Autocomplete activity
                                // For now, do nothing, which allows the user to click on NEXT and try again.
                            }
                            is SelectSportsActivity.Companion.SelectSportsCancelledException -> {
                                // The user cancelled the Select Sports activity
                                // For now, do nothing, which allows the user to click on NEXT and try again.
                            }
                            else -> {
                                // Some other error occurred
                                ErrorHandlerLauncher().launchExtrasErrorHandler(
                                    context,
                                    "An error occurred while signing up. Please try again."
                                )
                                // To notify test framework that something bad happened
                                databaseStateSending.completeExceptionally(it)
                            }
                        }
                        throw it
                    }
                }
            )
            { Text("SELECT HOME LOCATION") }
        }
    }
}