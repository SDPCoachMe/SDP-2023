package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataFromDatabase = (application as CoachMeApplication).database.getData()
        setContent {
            CoachMeTheme {
                GreetingForm(dataFromDatabase)
            }
        }
    }
}

@Composable
fun GreetingForm(dataFromDatabase: String) {
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
        Text("Data from database: $dataFromDatabase", modifier = Modifier.testTag("dbText"))
    }
}