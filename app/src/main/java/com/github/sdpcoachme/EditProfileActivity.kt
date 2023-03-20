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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

/**
 * Activity used to view and edit the user's profile.
 */
class EditProfileActivity : ComponentActivity() {

    private lateinit var database: Database
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO handle null better
        val email = intent.getStringExtra("email") ?: "no valid email"

        database = (application as CoachMeApplication).database

        // TODO temporary solution to cast to UserInfo
        val futureUserInfo: CompletableFuture<UserInfo> = database.getUser(email).thenApply {
            val map = it as Map<*, *>
            UserInfo(
                map["firstName"] as String,
                map["lastName"] as String,
                map["email"] as String,
                map["phone"] as String,
                map["location"] as String,
                map["coach"] as Boolean,
                emptyList()
            )
        }

        setContent {
            CoachMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Profile(email, futureUserInfo)
                }
            }
        }
    }
}

/**
 * Composable used to display the user's profile.
 */
@Composable
fun Profile(email: String, futureUserInfo: CompletableFuture<UserInfo>) {
    val database = (LocalContext.current.applicationContext as CoachMeApplication).database

    // bind those to database
    var isEditing by remember { mutableStateOf(false) }
    var fname by remember { mutableStateOf("") }
    var lname by remember { mutableStateOf("") }
    var favsport by remember { mutableStateOf("") }
    var isCoach by remember { mutableStateOf(false) }
    var switchCoachClient by remember { mutableStateOf(false) }

    var f by remember { mutableStateOf(futureUserInfo)}

    f.thenAccept { newUser ->
        if (newUser != null) {
            fname = newUser.firstName
            lname = newUser.lastName
            // TODO temporary sports handling
            favsport = ""
            isCoach = newUser.isCoach
            f = CompletableFuture.completedFuture(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("column"),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TitleRow(isCoach)
        EmailRow(email)

        ProfileRow(rowName = "First name", isEditing = isEditing, leftTextPadding = 45.dp,
            value = fname, onValueChange = { newValue -> fname = newValue })
        ProfileRow(rowName = "Last name", isEditing = isEditing, leftTextPadding = 45.dp,
            value = lname, onValueChange = { newValue -> lname = newValue })
        ProfileRow(rowName = "Favorite sport", isEditing = isEditing, leftTextPadding = 20.dp,
            value = favsport, onValueChange = { newValue -> favsport = newValue })

        if (isEditing) {
            SwitchClientCoachRow(isCoach, switchCoachClient) { switchCoachClient = it }

            // save button
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("save button"),
                onClick = {
                    isEditing = false
                    // TODO temporary sports handling
                    isCoach = isCoach xor switchCoachClient
                    switchCoachClient = false
                    val newUser = UserInfo(fname, lname, email, "", "", isCoach, listOf())
                    database.addUser(newUser)
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
fun SwitchClientCoachRow(isCoach: Boolean, switchCoachClient: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .absolutePadding(20.dp, 0.dp, 0.dp, 10.dp)
            .testTag("switch client coach row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start

    ) {
        Text("I would like to become a " + if (isCoach) "client" else "coach")
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = switchCoachClient,
            onCheckedChange = { onValueChange(it) }
        )
    }
}

/**
 * Composable used to display the profile title and the user's profile picture.
 */
@Composable
fun TitleRow(isCoach: Boolean) {
    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 20.dp, 0.dp, 10.dp)
            .testTag("title row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text(
                text = "My Profile",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCoach) "Coach" else "Client",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
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
