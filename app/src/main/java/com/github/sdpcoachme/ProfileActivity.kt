package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.CLIENT_COACH
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.COACH_CLIENT_INFO
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LOCATION
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.SELECTED_SPORTS
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocation
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity
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
        class SelectedSportsRowTag(tag: String) {
            val LABEL = "${tag}Text"
            val ROW = "${tag}Row"
        }
        class Buttons {
            companion object {
                const val SAVE = "saveButton"
                const val EDIT = "editButton"
                const val MESSAGE_COACH = "messageCoachButton"
                const val SELECT_SPORTS = "selectSportsButton"
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
            val LOCATION = EditableProfileRowTag("location")
            val CLIENT_COACH = SwitchClientCoachRowTag("clientCoach")
            val SELECTED_SPORTS = SelectedSportsRowTag("selectedSports")

        }
    }

    private lateinit var database: Database
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        val isViewingCoach = intent.getBooleanExtra("isViewingCoach", false)
        email =
            if (isViewingCoach) intent.getStringExtra("email").toString()
            else database.getCurrentEmail()


        // note : in the case where a coach is viewed but the email is not found
        // the value of the email will be "null" (see toString method of String)
        if (email.isEmpty() || email == "null") {
            val errorMsg = "Profile editing did not receive an email address." +
                    "\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val futureUserInfo = database.getUser(email)

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
    // TODO: fix this composable
    val context = LocalContext.current
    val database = (LocalContext.current.applicationContext as CoachMeApplication).database

    // bind those to database
    var isEditing by remember { mutableStateOf(false) }
    var fname by remember { mutableStateOf("") }
    var lname by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isCoach by remember { mutableStateOf(false) }
    var switchCoachClient by remember { mutableStateOf(false) }

    var userInfo by remember { mutableStateOf(UserInfo()) }

    var f by remember { mutableStateOf(futureUserInfo)}

    f.thenAccept { newUser ->
        if (newUser != null) {
            fname = newUser.firstName
            lname = newUser.lastName
            location = newUser.location.address
            isCoach = newUser.coach
            f = CompletableFuture.completedFuture(null)
            userInfo = newUser
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

        ProfileRow(rowName = "First name", tag = FIRST_NAME, isEditing = isEditing, leftTextPadding = 37.dp,
            value = fname, onValueChange = { newValue -> fname = newValue })
        ProfileRow(rowName = "Last name", tag = LAST_NAME, isEditing = isEditing, leftTextPadding = 37.dp,
            value = lname, onValueChange = { newValue -> lname = newValue })
        ProfileRow(rowName = "Location", tag = LOCATION, isEditing = isEditing, leftTextPadding = 50.dp,
            value = location, onValueChange = { newValue -> location = newValue })

        SportsRow(rowName = "Sports", tag = SELECTED_SPORTS, userInfo = userInfo)

        if (isViewingCoach) {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ProfileActivity.TestTags.Buttons.MESSAGE_COACH),
                onClick = {
                    val userEmail = database.getCurrentEmail()
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("currentUserEmail", userEmail)
                    intent.putExtra("toUserEmail", email)
                    context.startActivity(intent)
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
                    isCoach = isCoach xor switchCoachClient
                    switchCoachClient = false
                    val newUser = UserInfo(fname, lname, email, "", UserLocation(), isCoach, userInfo.sports, emptyList())
                    database.updateUser(newUser)
                }
            ) {
                Text(text = "Save changes")
            }
        } else {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ProfileActivity.TestTags.Buttons.SELECT_SPORTS),
                onClick = {
                    val selSportsIntent = Intent(context, SelectSportsActivity::class.java)
                    selSportsIntent.putExtra("isEditingProfile", true)
                    context.startActivity(selSportsIntent)
                }
            ) {
                Text(text = "Change sports")
            }

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
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag(tag.ROW),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = "$rowName: ", modifier = Modifier
            .defaultMinSize(50.dp, 20.dp)
            .testTag(tag.LABEL))
        if (isEditing) {
            TextField(
                modifier = Modifier
                    .absolutePadding(leftTextPadding, 0.dp, 0.dp, 0.dp)
                    .defaultMinSize(150.dp, 40.dp)
                    .testTag(tag.FIELD),
                value = value,
                onValueChange = { newValue -> onValueChange(newValue) },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (rowName == "Location")
                            focusManager.clearFocus()
                        else
                            focusManager.moveFocus(FocusDirection.Down)
                    }
                ))
        } else {
            Text(
                modifier = Modifier
                    .absolutePadding(leftTextPadding + 6.dp, 0.dp, 0.dp, 0.dp)
                    .testTag(tag.TEXT),
                text = value)
        }
    }
}

@Composable
fun SportsRow(rowName: String, tag: ProfileActivity.TestTags.SelectedSportsRowTag, userInfo: UserInfo) {
    Row(
        modifier = Modifier
            .absolutePadding(20.dp, 10.dp, 20.dp, 10.dp)
            .testTag(tag.ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = "$rowName: ", modifier = Modifier
            .defaultMinSize(125.dp, 20.dp)
            .testTag(tag.LABEL))
        userInfo.sports.map {
            Icon(
                imageVector = it.sportIcon,
                tint = Color.Gray,
                contentDescription = it.sportName,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
