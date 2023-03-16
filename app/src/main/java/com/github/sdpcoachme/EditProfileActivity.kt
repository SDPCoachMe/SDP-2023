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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.ui.theme.CoachMeTheme

/**
 * Activity used to view and edit the user's profile.
 */
class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("email")
            ?: throw IllegalStateException("No email passed to EditProfileActivity")

        setContent {
            CoachMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Profile(email)
                }
            }
        }
    }
}

/**
 * Composable used to display the user's profile.
 */
@Composable
fun Profile(email: String) {
    // bind those to database
    var isEditing by remember { mutableStateOf(false) }
    var fname by remember { mutableStateOf("Damian") }
    var lname by remember { mutableStateOf("Kopp") }
    var favsport by remember { mutableStateOf("Jogging") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("column"),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TitleRow()
        EmailRow(email)

        ProfileRow(rowName = "First name", isEditing = isEditing, leftTextPadding = 45.dp,
            value = fname, onValueChange = { newValue -> fname = newValue })
        ProfileRow(rowName = "Last name", isEditing = isEditing, leftTextPadding = 45.dp,
            value = lname, onValueChange = { newValue -> lname = newValue })
        ProfileRow(rowName = "Favorite sport", isEditing = isEditing, leftTextPadding = 20.dp,
            value = favsport, onValueChange = { newValue -> favsport = newValue })

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

/**
 * Composable used to display the profile title and the user's profile picture.
 */
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

/**
 * Composable used to display the user's email address.
 */
@Composable
fun EmailRow(email: String) {
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
            text = email,
            modifier = Modifier
                .absolutePadding(80.dp, 0.dp, 0.dp, 0.dp)
                .testTag("email address"),
        )
    }
}

/**
 * Composable used to display a row of the user profile.
 *
 * @param rowName the name of the row
 * @param isEditing whether the user is currently editing their profile
 * @param leftTextPadding the amount of padding to the left of the text field
 * @param value the value of the row
 * @param onValueChange the function to call when the value of the row changes
 */
@Composable
fun ProfileRow(rowName: String, isEditing: Boolean, leftTextPadding: Dp, value: String, onValueChange: (String) -> Unit) {
    val lowercaseRowName = rowName.lowercase()
    Row(
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag("$lowercaseRowName row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = "$rowName: ", modifier = Modifier.defaultMinSize(50.dp, 20.dp))
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(leftTextPadding, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag("editable $lowercaseRowName"),
                value = value,
                onValueChange = { newValue -> onValueChange(newValue) },
                singleLine = true,
                maxLines = 1)
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(leftTextPadding + 6.dp, 0.dp, 0.dp, 0.dp)
                    .testTag("saved $lowercaseRowName"),
                text = value)
        }
    }
}
