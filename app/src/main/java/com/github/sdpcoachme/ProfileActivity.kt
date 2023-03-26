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
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.CLIENT_COACH
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.COACH_CLIENT_INFO
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.SPORT
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

/**
 * Activity used to view and edit the user's profile or view a coach's profile.
 */
class ProfileActivity : ComponentActivity() {

    class TestTags {
        class EditableProfileRowTag(tag: String) {
            val FIELD = "${tag}TextField"
            val TEXT = "${tag}Text"
            val LABEL = "${tag}Label"
            val ROW = "${tag}Row"
        }
        class UneditableProfileRowTag(tag: String) {
            val TEXT = "${tag}Text"
            val LABEL = "${tag}Label"
            val ROW = "${tag}Row"
        }
        class SwitchClientCoachRowTag(tag: String) {
            val SWITCH = "${tag}Switch"
            val TEXT = "${tag}Text"
            val ROW = "${tag}Row"
        }
        class Buttons {
            companion object {
                const val SAVE = "saveButton"
                const val EDIT = "editButton"
                const val MESSGE_COACH = "messageCoachButton"
            }
        }
        companion object {
            const val TITLE_ROW = "titleRow"
            const val PROFILE_LABEL = "profileLabel"
            const val PROFILE_PICTURE = "profilePicture"
            const val PROFILE_COLUMN = "profileColumn"
            const val COACH_CLIENT_INFO = "coachClientInfo"
            val EMAIL = UneditableProfileRowTag("email")
            val FIRST_NAME = EditableProfileRowTag("firstName")
            val LAST_NAME = EditableProfileRowTag("lastName")
            val SPORT = EditableProfileRowTag("sport")
            val CLIENT_COACH = SwitchClientCoachRowTag("clientCoach")

        }
    }

    private lateinit var database: Database
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("email")
        val isViewingCoach = intent.getBooleanExtra("isViewingCoach", false)

        if (email == null) {
            val errorMsg = "Profile editing did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {

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
            }.exceptionally { null }

            setContent {
                CoachMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        Profile(email, futureUserInfo, isViewingCoach)
                    }
                }
            }
        }
    }
}

/**
 * Composable used to display the user's profile.
 */
@Composable
fun Profile(email: String, futureUserInfo: CompletableFuture<UserInfo>, isViewingCoach: Boolean) {
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
            .testTag(ProfileActivity.TestTags.PROFILE_COLUMN),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TitleRow(isCoach, isViewingCoach)
        EmailRow(email)

        ProfileRow(rowName = "First name", tag = FIRST_NAME, isEditing = isEditing, leftTextPadding = 45.dp,
            value = fname, onValueChange = { newValue -> fname = newValue })
        ProfileRow(rowName = "Last name", tag = LAST_NAME, isEditing = isEditing, leftTextPadding = 45.dp,
            value = lname, onValueChange = { newValue -> lname = newValue })
        ProfileRow(rowName = "Favorite sport", tag = SPORT, isEditing = isEditing, leftTextPadding = 20.dp,
            value = favsport, onValueChange = { newValue -> favsport = newValue })

        if (isViewingCoach) {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ProfileActivity.TestTags.Buttons.MESSGE_COACH),
                onClick = {
                    // For the moment, nothing happens
                    // but in the future this could open the in app messenger with the coach
                }
            ) {
                Text(text = "Message coach")
            }
        } else if (isEditing) {
            SwitchClientCoachRow(isCoach, switchCoachClient) { switchCoachClient = it }

            // save button
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ProfileActivity.TestTags.Buttons.SAVE),
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
                    .testTag(ProfileActivity.TestTags.Buttons.EDIT),
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
            .testTag(CLIENT_COACH.ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start

    ) {
        Text("I would like to become a " + if (isCoach) "client" else "coach",
            modifier = Modifier.testTag(CLIENT_COACH.TEXT))
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = switchCoachClient,
            onCheckedChange = { onValueChange(it) },
            modifier = Modifier.testTag(CLIENT_COACH.SWITCH)
        )
    }
}

/**
 * Composable used to display the profile title and the user's profile picture.
 */
@Composable
fun TitleRow(isCoach: Boolean, isViewingCoach: Boolean) {
    Row (
        modifier = Modifier
            .absolutePadding(20.dp, 20.dp, 0.dp, 10.dp)
            .testTag(ProfileActivity.TestTags.TITLE_ROW),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            if (isViewingCoach) {
                Text(
                    modifier = Modifier.testTag(PROFILE_LABEL),
                    text = "Coach's Profile",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    modifier = Modifier.testTag(PROFILE_LABEL),
                    text = "My Profile",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isCoach) "Coach" else "Client",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.testTag(COACH_CLIENT_INFO)
                )
            }
        }
        Box(
//            modifier = Modifier
//                .absolutePadding(0.dp, 0.dp, 0dp, 0.dp)
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(0.dp, 0.dp, 25.dp, 0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Profile Pic",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
                    .absolutePadding(0.dp, 0.dp, 0.dp, 0.dp)
                    .testTag(ProfileActivity.TestTags.PROFILE_PICTURE)
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
            .testTag(EMAIL.ROW),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ){
        Text(modifier = Modifier.testTag(EMAIL.LABEL), text = "Email: ")
        // replace this Text with read-only TextField?
        Text(
            text = email,
            modifier = Modifier
                .absolutePadding(80.dp, 0.dp, 0.dp, 0.dp)
                .testTag(EMAIL.TEXT),
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
fun ProfileRow(rowName: String, tag: ProfileActivity.TestTags.EditableProfileRowTag, isEditing: Boolean, leftTextPadding: Dp, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag(tag.ROW),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = "$rowName: ", modifier = Modifier.defaultMinSize(50.dp, 20.dp).testTag(tag.LABEL))
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(leftTextPadding, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag(tag.FIELD),
                value = value,
                onValueChange = { newValue -> onValueChange(newValue) },
                singleLine = true,
                maxLines = 1)
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(leftTextPadding + 6.dp, 0.dp, 0.dp, 0.dp)
                    .testTag(tag.TEXT),
                text = value)
        }
    }
}
