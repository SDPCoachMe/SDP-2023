package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                GreetingForm()
            }
        }
    }
}

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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        GreetingForm()
    }
}