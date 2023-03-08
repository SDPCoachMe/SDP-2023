package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class EditProfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Profile()
                }
            }
        }
    }
}

@Composable
fun Profile() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TitleRow()
        EmailRow()
        FirstNameRow()
        LastNameRow()

        Row (
            modifier = Modifier.absolutePadding(20.dp, 10.dp, 0.dp, 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(text = "Favorite sport: ")
        }
    }
}

@Composable
fun TitleRow() {
    Row (
        modifier = Modifier.absolutePadding(20.dp, 20.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "My Profile",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmailRow() {
    Row (
        modifier = Modifier.absolutePadding(20.dp, 80.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "Email: ")
        // replace this Text with read-only TextField?
        Text(
            text = "damian.kopp@epfl.ch",
            modifier = Modifier.absolutePadding(80.dp, 0.dp, 0.dp, 0.dp),
        )
    }
}

@Composable
fun FirstNameRow() {
    // bind this to database
    var fname by remember { mutableStateOf(TextFieldValue("Damian")) }

    Row (
        modifier = Modifier.absolutePadding(20.dp, 10.dp, 20.dp, 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "First name: ")
        TextField(
            modifier = Modifier
                .absolutePadding(40.dp, 0.dp, 0.dp, 0.dp)
                .defaultMinSize(150.dp, 40.dp),
            value = fname,
            onValueChange = {
                fname = it
            },
            singleLine = true,
            maxLines = 1
        )
    }
}

@Composable
fun LastNameRow() {
    // bind this to database
    var lname by remember { mutableStateOf(TextFieldValue("Kopp")) }

    Row (
        modifier = Modifier.absolutePadding(20.dp, 10.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "Last name: ")
        TextField(
            modifier = Modifier
                .absolutePadding(40.dp, 0.dp, 0.dp, 0.dp)
                .defaultMinSize(150.dp, 40.dp),
            value = lname,
            onValueChange = {
                lname = it
            },
            singleLine = true,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        Profile()
    }
}