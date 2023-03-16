package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class SignupActivity : ComponentActivity() {

    private lateinit var database : Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database =  (application as CoachMeApplication).database
        setContent {
            CoachMeTheme {
                AccountForm()
            }
        }
    }

    @Composable
    fun AccountForm() {
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") } //todo how to accept only valid location ?? => Google Maps
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.testTag("firstName"),
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") }
            )
            TextField(
                modifier = Modifier.testTag("lastName"),
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") }
            )
            TextField( // todo to be removed and retrieved from google authentication variable
                modifier = Modifier.testTag("email"),
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            TextField(
                modifier = Modifier.testTag("phone"),
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") }
            )
            TextField(
                modifier = Modifier.testTag("location"),
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") }
            )
            Button(
                modifier = Modifier.testTag("registerButton"),
                onClick = {
                    // Add the new user to the database
                    val newUser = UserInfo(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        location = location,
                        sports = listOf() // todo add sports with MultiSelectListUI
                    )
                    database.addUser(newUser)
                })
            { Text("REGISTER") }
        }
    }
}

