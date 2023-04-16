package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.MESSAGE_COACH
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.COACH_CLIENT_INFO
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.COACH_SWITCH
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LOCATION
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PHONE
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.SPORTS
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.location.autocomplete.LocationAutocompleteHandler
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

/**
 * Activity used to view and edit the user's profile or view a coach's profile.
 */
class ProfileActivity : ComponentActivity() {

    class TestTags {
        class Buttons {
            companion object {
                const val MESSAGE_COACH = "messageCoachButton"
            }
        }
        companion object {
            const val PROFILE_LABEL = "profileLabel"
            const val PROFILE_PICTURE = "profilePicture"
            const val COACH_CLIENT_INFO = "coachClientInfo"

            const val EMAIL = "email"
            const val FIRST_NAME = "firstName"
            const val LAST_NAME = "lastName"
            const val PHONE = "phone"
            const val LOCATION = "location"
            const val SPORTS = "sports"
            const val COACH_SWITCH = "coachSwitch"

        }
    }

    private lateinit var database: Database
    private lateinit var email: String
    private lateinit var locationAutocompleteHandler: LocationAutocompleteHandler
    private lateinit var editTextHandler: (Intent) -> CompletableFuture<String>

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

            // Set up handler for calls to location autocomplete
            locationAutocompleteHandler = (application as CoachMeApplication).locationAutocompleteHandler(this, this)

            // Set up handler for calls to edit text activity
            editTextHandler = EditTextActivity.getHandler(this)

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

    /**
     * Composable used to display the user's profile.
     */
    @Composable
    fun Profile(email: String, futureUserInfo: CompletableFuture<UserInfo>, isViewingCoach: Boolean) {

        val context = LocalContext.current
        val database = (LocalContext.current.applicationContext as CoachMeApplication).database

        var userInfo by remember { mutableStateOf(UserInfo()) }

        // Make sure the userInfo variable is updated when the futureUserInfo completes
        LaunchedEffect(futureUserInfo) {
            userInfo = futureUserInfo.await()
        }

        /**
         * Saves the user's profile information to the database, and then updates the local userInfo
         * state which refreshes the UI. Already handles exceptions due to cancelling of edit text
         * activity or location autocomplete activity, and redirects to error handler when necessary.
         */
        fun saveUserInfo(futureNewUserInfo: CompletableFuture<UserInfo>): CompletableFuture<Void> {
            return futureNewUserInfo
                .thenCompose { newUserInfo ->
                    database.updateUser(newUserInfo).thenAccept { userInfo = newUserInfo }
                }
                .exceptionally {
                    when (it.cause) {
                        is LocationAutocompleteHandler.AutocompleteCancelledException -> {
                            // The user cancelled the Places Autocomplete activity
                            // For now, do nothing, which allows the user to try again
                        }
                        is EditTextActivity.Companion.EditTextCancelledException -> {
                            // The user cancelled the EditText activity
                            // For now, do nothing, which allows the user to try again
                        }
                        else -> {
                            // Some other error occurred
                            ErrorHandlerLauncher().launchExtrasErrorHandler(
                                context,
                                "An error occurred while editing the profile. Please try again."
                            )
                        }
                    }
                    throw it
                }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            TitleRow(userInfo.coach, isViewingCoach)
            Spacer(modifier = Modifier.height(10.dp))
            TextRow(
                label = "EMAIL",
                tag = EMAIL,
                value = email,
                onClick = {
                    // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                }
            )
            Divider(startIndent = 20.dp)
            TextRow(
                label = "FIRST NAME",
                tag = FIRST_NAME,
                value = userInfo.firstName,
                onClick = {
                    if (!isViewingCoach) {
                        val future = editTextHandler(EditTextActivity.getIntent(
                            context = context,
                            initialValue = userInfo.firstName,
                            label = "First name"
                        )).thenApply { firstName ->
                            userInfo.copy(firstName = firstName)
                        }
                        // Update database
                        saveUserInfo(future)
                    } else {
                        // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                    }
                }
            )
            Divider(startIndent = 20.dp)
            TextRow(
                label = "LAST NAME",
                tag = LAST_NAME,
                value = userInfo.lastName,
                onClick = {
                    if (!isViewingCoach) {
                        val future = editTextHandler(EditTextActivity.getIntent(
                            context = context,
                            initialValue = userInfo.lastName,
                            label = "Last name"
                        )).thenApply { lastName ->
                            userInfo.copy(lastName = lastName)
                        }
                        // Update database
                        saveUserInfo(future)
                    } else {
                        // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                    }
                }
            )
            Divider(startIndent = 20.dp)
            TextRow(
                label = "PHONE",
                tag = PHONE,
                value = userInfo.phone,
                onClick = {
                    if (!isViewingCoach) {
                        val future = editTextHandler(EditTextActivity.getIntent(
                            context = context,
                            initialValue = userInfo.phone,
                            label = "Phone"
                        )).thenApply { phone ->
                            userInfo.copy(phone = phone)
                        }
                        // Update database
                        saveUserInfo(future)
                    } else {
                        // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                    }
                }
            )
            Divider(startIndent = 20.dp)
            TextRow(
                label = "LOCATION",
                tag = LOCATION,
                value = userInfo.location.address,
                onClick = {
                    if (!isViewingCoach) {
                        val future = locationAutocompleteHandler.launch().thenApply { location ->
                            userInfo.copy(location = location)
                        }
                        // Update database
                        saveUserInfo(future)
                    } else {
                        // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                    }
                }
            )
            Divider(startIndent = 20.dp)
            SportsRow(
                label = "SPORTS",
                tag = SPORTS,
                value = userInfo.sports,
                onClick = {
                    if (!isViewingCoach) {
                        val selSportsIntent = Intent(context, SelectSportsActivity::class.java)
                        selSportsIntent.putExtra("isEditingProfile", true)
                        context.startActivity(selSportsIntent)
                    } else {
                        // Uneditable, for now, do nothing (might allow to copy to clipboard on click)
                    }
                }
            )

            if (isViewingCoach) {
                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(MESSAGE_COACH),
                    onClick = {
                        // For the moment, nothing happens
                        // but in the future this could open the in app messenger with the coach
                    }
                ) {
                    Text(text = "MESSAGE COACH")
                }
            } else {
                Divider(startIndent = 20.dp)
                SwitchClientCoachRow(
                    value = userInfo.coach,
                    onValueChange = {
                        saveUserInfo(CompletableFuture.completedFuture(userInfo.copy(coach = it)))
                    }
                )
            }
        }
    }
}

/**
 * Composable used to display the profile title and the user's profile picture.
 */
@Composable
fun TitleRow(isCoach: Boolean, isViewingCoach: Boolean) {
    Row (
        modifier = Modifier.padding(20.dp, 20.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text(
                modifier = Modifier.testTag(PROFILE_LABEL),
                text = "${if (isViewingCoach) "Coach's" else "My"} Profile",
                style = MaterialTheme.typography.h4
            )
            if (!isViewingCoach) {
                Text(
                    text = if (isCoach) "Coach" else "Client",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.testTag(COACH_CLIENT_INFO)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 25.dp, 0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
                    .padding(0.dp, 0.dp, 0.dp, 0.dp)
                    .testTag(ProfileActivity.TestTags.PROFILE_PICTURE)
            )
        }
    }
}

/**
 * Composable used to display a row with a label and a value.
 */
@Composable
fun AttributeRow(
    label: String,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .padding(20.dp, 10.dp, 20.dp, 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier
                .requiredHeight(22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            style = MaterialTheme.typography.overline,
            fontSize = 8.sp
        )
    }
}

/**
 * Composable used to display a row with a label and a text value.
 */
@Composable
fun TextRow(
    label: String,
    tag: String,
    onClick: () -> Unit = {},
    value: String
) {
    AttributeRow(
        label = label,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.testTag(tag),
            text = value,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Composable used to display a row with a label and a list of sports icons.
 */
@Composable
fun SportsRow(
    label: String,
    tag: String,
    onClick: () -> Unit = {},
    value: List<Sports>
) {
    AttributeRow(
        label = label,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .testTag(tag)
                .padding(0.dp, 0.dp, 0.dp, 2.5.dp)
        ) {
            value.map {
                Icon(
                    imageVector = it.sportIcon,
                    contentDescription = it.sportName,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

/**
 * Composable used to display the row with the switch to change the coach status and its description.
 */
@Composable
fun SwitchClientCoachRow(value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically

    ) {
        Text(
            text = "I would like others to see me as a coach",
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(20.dp, 0.dp, 0.dp, 0.dp)
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
            modifier = Modifier
                .testTag(COACH_SWITCH)
                .padding(0.dp, 0.dp, 20.dp, 0.dp)
        )
    }
}