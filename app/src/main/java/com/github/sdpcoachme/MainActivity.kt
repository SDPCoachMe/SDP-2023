package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.sdpcoachme.ui.theme.CoachMeTheme


class MainActivity : ComponentActivity() {


    private lateinit var db: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = (application as CoachMeApplication).database
        setContent {
            CoachMeTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FirebaseForm()
                    GreetingForm()
                }
            }
        }
    }

    //TODO put test tags in separate class


    /**
     * A form for testing the database
     */
    @Composable
    fun FirebaseForm() {
        var phoneText by remember { mutableStateOf("") }
        var emailText by remember { mutableStateOf("") }
        Column( //modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(//modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(modifier = Modifier.testTag("getButton"),
                    onClick = {
                        db.get(phoneText).thenAccept { emailText = it as String }
                    },)
                {
                    Text("get")
                }
                Button(modifier = Modifier.testTag("setButton"),
                    onClick = {
                        db.set(phoneText, emailText)
                    },)
                {
                    Text("set")
                }

            }
            TextField(
                modifier = Modifier.testTag("phoneTextfield"),
                value = phoneText,
                onValueChange = { phoneText = it },
                label = { Text("Phone") }
            )
            TextField(
                modifier = Modifier.testTag("emailTextfield"),
                value = emailText,
                onValueChange = { emailText = it },
                label = { Text("Email") }
            )

        }

    }

    /**
     * A form for testing the greeting activity
     */
    @Composable
    fun GreetingForm() {
        var text by remember { mutableStateOf("") }
        val context = LocalContext.current

        Column(
            //modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.testTag("textfield"),
                value = text,
                onValueChange = { text = it },
                label = { Text("Your name") }
            )
            Button(
                modifier = Modifier.testTag("button"),
                onClick = {
                    val intent = Intent(context, GreetingActivity::class.java)
                    intent.putExtra("name", text)
                    context.startActivity(intent)
                })
            { Text("DISPLAY MESSAGE") }
        }
    }
}