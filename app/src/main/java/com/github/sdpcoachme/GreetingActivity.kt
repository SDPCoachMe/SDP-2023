package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class GreetingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                intent.getStringExtra("name")?.let { Greeting(it) }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    CoachMeTheme {
        Greeting("John")
    }
}