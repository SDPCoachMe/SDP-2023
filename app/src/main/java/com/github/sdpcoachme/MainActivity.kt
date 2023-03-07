package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                AccountForm()
            }
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
    val context = LocalContext.current

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
        TextField(
            modifier = Modifier.testTag("sport"),
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
            modifier = Modifier.testTag("register Button"),
            onClick = {
                //todo
            })
        { Text("REGISTER") }
    }
}

/*
@Composable
fun AccountElem(label: String, onValueChange: (String) -> Unit, testTag: String) {
    TextField(
        modifier = Modifier.testTag(testTag),
        value = ,
        onValueChange = onValueChange,
        label = { Text(label) }
    )
}
*/

@Composable
fun GreetingForm() {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
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