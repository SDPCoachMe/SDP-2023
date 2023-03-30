package com.github.sdpcoachme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.concurrent.CompletableFuture

class SignupActivity : ComponentActivity() {

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
    private lateinit var placesClient : PlacesClient
    private lateinit var placesAutocompleteStartForResult : ActivityResultLauncher<Intent>
    private var autocompleteResult = CompletableFuture<Place>()

    // Used to handle places autocomplete activity errors
    class PlacesAutocompleteFailed(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class PlacesAutocompleteCancelled(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("email")

        if (email == null) {
            val errorMsg = "The signup page did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            // Get database instance
            database = (application as CoachMeApplication).database

            // Set up call to Places API and callback
            placesClient = Places.createClient(this)
            placesAutocompleteStartForResult = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                // Handle the result from the Places Autocomplete activity that was started
                result: ActivityResult ->
                    when (result.resultCode) {
                        Activity.RESULT_OK -> {
                            result.data?.let {
                                val place = Autocomplete.getPlaceFromIntent(it)
                                autocompleteResult.complete(place)
                            }
                        }
                        Activity.RESULT_CANCELED -> {
                            // The user canceled the operation
                            autocompleteResult.completeExceptionally(PlacesAutocompleteCancelled())
                        }
                        else -> {
                            // There was an unknown error
                            // Log.d("AUTOCOMPLETE_STATUS", "Status ${Autocomplete.getStatusFromIntent(result.data!!)}") // access error details
                            autocompleteResult.completeExceptionally(PlacesAutocompleteFailed())
                        }
                    }
            }

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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.FIRST_NAME),
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") }
            )
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.LAST_NAME),
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") }
            )
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.PHONE),
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") }
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
                    // Launch autocomplete activity, then wait for result
                    val fields = listOf(Place.Field.ID, Place.Field.NAME)
                    val autocompleteIntent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(context)
                    placesAutocompleteStartForResult.launch(autocompleteIntent)
                    autocompleteResult.thenCompose {
                        place ->
                            // Add the new user to the database
                            val newUser = UserInfo(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                location = place.id!!, // it can't be null anyways
                                coach = isCoach,
                                // sports added later in SelectSportsActivity
                                sports = listOf()
                            )
                            database.addUser(newUser)
                    }.thenApply {
                        // Launch SelectSportsActivity
                        val intent = Intent(context, SelectSportsActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    }.exceptionally {
                        when (it) {
                            is PlacesAutocompleteCancelled -> {
                                // The user cancelled the Places Autocomplete activity
                                // For now, do nothing, which allows the user to click on NEXT and try again.
                            }
                            else -> {
                                // Some other error occurred
                                ErrorHandlerLauncher().launchExtrasErrorHandler(
                                    context,
                                    "An error occurred while signing up. Please try again."
                                )
                            }
                        }
                    }
                }
            )
            { Text("SELECT LOCATION") }
        }
    }
}
