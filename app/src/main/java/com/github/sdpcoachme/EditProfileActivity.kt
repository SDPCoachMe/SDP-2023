package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().testTag("column"),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TitleRow()
        EmailRow()
        FirstNameRow(isEditing)
        LastNameRow(isEditing)
        FavSportRow(isEditing)

        if (isEditing) {
            // save button
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("save button"),
                onClick = {
                    isEditing = false
                }
            ) {
                Text(text = "Save changes")
            }
        } else {
            // edit button
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("edit button"),
                onClick = {
                    isEditing = true
                }
            ) {
                Text(text = "Edit")
            }
        }


    }
}

@Composable
fun TitleRow() {
    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 20.dp, 0.dp, 10.dp)
            .testTag("title row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "My Profile",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .absolutePadding(100.dp, 0.dp, 0.dp, 0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Profile Pic",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
                    .absolutePadding(0.dp, 0.dp, 0.dp, 0.dp)
                    .testTag("profile pic")
            )
        }
    }
}

@Composable
fun EmailRow() {
    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 80.dp, 0.dp, 10.dp)
            .testTag("email row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "Email: ")
        // replace this Text with read-only TextField?
        Text(
            text = "damian.kopp@epfl.ch",
            modifier = Modifier
                .absolutePadding(80.dp, 0.dp, 0.dp, 0.dp)
                .testTag("email address"),
        )
    }
}

@Composable
fun FirstNameRow(isEditing: Boolean) {
    // bind this to database
    var fname by remember { mutableStateOf(TextFieldValue("Damian")) }

    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag("first name row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "First name: ")
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(40.dp, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag("editable first name"),
                value = fname,
                onValueChange = {
                    fname = it
                },
                singleLine = true,
                maxLines = 1)
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(45.dp, 0.dp, 0.dp, 0.dp)
                    .testTag("saved first name"),
                text = fname.text)
        }
    }
}

@Composable
fun LastNameRow(isEditing: Boolean) {
    // bind this to database
    var lname by remember { mutableStateOf(TextFieldValue("Kopp")) }

    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag("last name row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "Last name: ")
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(40.dp, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag("editable last name"),
                value = lname,
                onValueChange = {
                    lname = it
                },
                singleLine = true,
                maxLines = 1)
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(45.dp, 0.dp, 0.dp, 0.dp)
                    .testTag("saved last name"),
                text = lname.text)
        }
    }
}

@Composable
fun FavSportRow(isEditing: Boolean) {
    // bind this to database
    var favsport by remember { mutableStateOf(TextFieldValue("Jogging")) }

    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag("fav sport row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(text = "Favorite sport: ")
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(15.dp, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag("editable fav sport"),
                value = favsport,
                onValueChange = {
                    favsport = it
                },
                singleLine = true,
                maxLines = 1)
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(20.dp, 0.dp, 0.dp, 0.dp)
                    .testTag("saved fav sport"),
                text = favsport.text)
        }
    }
}

/*
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        Profile()
    }
}*/
