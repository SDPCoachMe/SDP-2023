package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorActivity
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class SignupActivity : ComponentActivity() {

    class TestTags {
        class TextFields {
            companion object {
                const val FIRST_NAME = "firstName"
                const val LAST_NAME = "lastName"
                const val PHONE = "phone"
                const val LOCATION = "location"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database

        val email = intent.getStringExtra("email")

        if (email == null) {
            val intent = Intent(this, IntentExtrasErrorActivity::class.java)
            intent.putExtra("errorMsg", "The signup page did not receive an email address.\n Please return to the login page and try again.")
            startActivity(intent)
        } else {

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
        var location by remember { mutableStateOf("") } //todo how to accept only valid location ?? => Google Maps
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
            TextField(
                modifier = Modifier.testTag(TestTags.TextFields.LOCATION),
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") }
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
                    // Add the new user to the database
                    val newUser = UserInfo(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        location = location,
                        isCoach = isCoach,
                        sports = listOf() // todo add sports with MultiSelectListUI
                    )
                    println("new user: $newUser")
                    database.addUser(newUser).thenApply {
                        val intent = Intent(context, DashboardActivity::class.java)
                        intent.putExtra("email", newUser.email)
                        startActivity(intent)
                    }.exceptionally {
                        // TODO handle the exception
                    }
                }
            )
            { Text("REGISTER") }
        }
    }
}

