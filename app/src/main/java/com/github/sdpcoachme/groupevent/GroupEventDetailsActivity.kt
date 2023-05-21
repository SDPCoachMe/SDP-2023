package com.github.sdpcoachme.groupevent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.CHAT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.JOIN_EVENT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_DAY
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_DESCRIPTION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_LOCATION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_MONTH
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_SPORT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_TIME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.ORGANIZER_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Tabs.Companion.ABOUT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Tabs.Companion.PARTICIPANTS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.ui.ImageData
import com.github.sdpcoachme.ui.SmallListItem
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

/**
 * Activity that displays the details of a group event.
 */
class GroupEventDetailsActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val TITLE = "title"
            const val EVENT_NAME = "eventName"
            const val EVENT_SPORT = "eventSport"
            const val ORGANIZER_NAME = "organizerName"
            const val EVENT_LOCATION = "eventLocation"
            const val EVENT_TIME = "eventTime"
            const val EVENT_DESCRIPTION = "eventDescription"
            const val EVENT_MONTH = "eventMonth"
            const val EVENT_DAY = "eventDay"
        }

        class Buttons {
            companion object {
                const val BACK = "back"
                const val JOIN_EVENT = "joinEvent"
                const val CHAT = "chat"
            }
        }

        class Tabs {
            companion object {
                const val ABOUT = "about"
                const val PARTICIPANTS = "participants"
            }
        }
    }

    companion object {

        private const val GROUP_EVENT_ID_KEY = "initialValue"

        /**
         * Creates an Intent that can be used to launch this activity.
         *
         * @param context The context of the caller activity.
         * @param groupEventId The id of the group event to display.
         * @return An Intent that can be used to launch this activity.
         */
        fun getIntent(
            context: Context,
            groupEventId: String
        ): Intent {
            val intent = Intent(context, GroupEventDetailsActivity::class.java)
            intent.putExtra(GROUP_EVENT_ID_KEY, groupEventId)
            return intent
        }
    }

    // To notify tests that the activity is ready
    lateinit var stateUpdated: CompletableFuture<Void>

    private lateinit var store: CachingStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store
        stateUpdated = CompletableFuture()

        // do not allow the activity to launch without an id. (double bang)
        val groupEventId = intent.getStringExtra(GROUP_EVENT_ID_KEY)!!

        setContent {

            var refreshUI by remember { mutableStateOf(false) }
            var groupEvent by remember { mutableStateOf<GroupEvent?>(null) }
            var organizer by remember { mutableStateOf<UserInfo?>(null) }
            var participants by remember { mutableStateOf<List<UserInfo>?>(null) }
            var currentUser by remember { mutableStateOf<UserInfo?>(null) }

            LaunchedEffect(refreshUI) {
                groupEvent = store.getGroupEvent(groupEventId).await()
                organizer = store.getUser(groupEvent!!.organizer).await()
                participants = groupEvent!!.participants.map {
                    store.getUser(it).await()
                }
                currentUser = store.getUser(store.getCurrentEmail().await()).await()
                stateUpdated.complete(null)
            }

            CoachMeTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Event details", modifier = Modifier.testTag(TITLE))
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { finish() },
                                        modifier = Modifier.testTag(BACK)
                                    ) {
                                        Icon(Icons.Filled.ArrowBack, "Back")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        Surface(
                            color = MaterialTheme.colors.background
                        ) {
                            Column(
                                modifier = Modifier.padding(padding)
                            ) {
                                if (groupEvent != null && currentUser != null
                                    && organizer != null && participants != null
                                ) {
                                    GroupEventDetailsLayout(
                                        groupEvent!!,
                                        organizer!!,
                                        currentUser!!,
                                        participants!!,
                                        onJoinEventClick = {
                                            store.registerForGroupEvent(groupEvent!!.groupEventId)
                                                .thenAccept {
                                                    // Will trigger the launched effect to refresh the UI
                                                    refreshUI = !refreshUI
                                                    // Tell the user that they have joined the event
                                                    val toast = Toast.makeText(
                                                        this@GroupEventDetailsActivity,
                                                        "You have succesfully joined the event!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    toast.show()
                                                    // Notifying tests is necessary here since the launched effect
                                                    // is not triggered in the tests for some weird reason
                                                    stateUpdated.complete(null)
                                                }
                                            // TODO: print something if the registration fails ?
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * This is the main layout for the GroupEventDetailsActivity.
 */
@Composable
fun GroupEventDetailsLayout(
    groupEvent: GroupEvent,
    organizer: UserInfo,
    currentUser: UserInfo,
    participants: List<UserInfo>,
    onJoinEventClick: () -> Unit,
) {
    val context = LocalContext.current

    val eventStart = LocalDateTime.parse(groupEvent.event.start)
    val eventEnd = LocalDateTime.parse(groupEvent.event.end)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                modifier = Modifier
                    .testTag(EVENT_NAME),
                text = groupEvent.event.name,
                style = MaterialTheme.typography.h4
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = groupEvent.event.sport.sportIcon,
                    contentDescription = groupEvent.event.sport.sportName,
                    modifier = Modifier
                        .size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    modifier = Modifier.testTag(EVENT_SPORT),
                    text = groupEvent.event.sport.sportName.uppercase(),
                    style = MaterialTheme.typography.overline.copy(fontSize = 12.sp),
                    color = Color.Gray
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // TODO : add weather info
                Spacer(modifier = Modifier.width(10.dp))
                DayBox(eventStart.dayOfMonth, eventStart.month)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "Organizer profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                        .padding(0.dp, 0.dp, 0.dp, 0.dp)
                )
                Text(
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                    text = "Organized by " ,
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
                )
                ClickableText(
                    modifier = Modifier.testTag(ORGANIZER_NAME),
                    text = AnnotatedString("${organizer.firstName} ${organizer.lastName}"),
                    style = MaterialTheme.typography.body1.copy(
                        color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                    ),
                    onClick = {
                        // Open organizer profile
                        // If the organizer is the current user, open the profile activity, but not in edit mode
                        // TODO : temporary until we update the way intents are handled in profile activity
                        val intent = Intent(context, ProfileActivity::class.java)
                        intent.putExtra("isViewingCoach", true)
                        intent.putExtra("email", organizer.email)
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            IconTextRow(
                icon = Icons.Default.Place,
                contentDescription = "Location",
                text = groupEvent.event.address.name,
                tag = EVENT_LOCATION,
                onClick = {
                    // Open google maps with the location of the event
                    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(groupEvent.event.address.name)}&query_place_id=${groupEvent.event.address.placeId}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // No google maps app found on device.
                        // For now, do nothing.
                        Log.e("GroupEventDetailsActivity", "Google Maps app not found")
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            IconTextRow(
                icon = Icons.Default.Schedule,
                contentDescription = "Time",
                text = "${eventStart.format(timeFormatter)}–${eventEnd.format(timeFormatter)}",
                tag = EVENT_TIME
            )
        }

        // Used for easier readability
        data class TabItem(
            val title: String,
            val tag: String,
            val content: @Composable () -> Unit
        )
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf(
            TabItem(
                "ABOUT",
                ABOUT
            ) {
                Text(
                    modifier = Modifier
                        .testTag(EVENT_DESCRIPTION)
                        .padding(20.dp),
                    text = groupEvent.event.description,
                )
            },
            TabItem(
                "PARTICIPANTS",
                PARTICIPANTS
            ) {
                Column {
                    participants.map {
                        SmallUserInfoListItem(
                            userInfo =
                                if (it.email == currentUser.email)
                                    it.copy(firstName = it.firstName, lastName = "${it.lastName} (me)")
                                else
                                    it,
                            onClick = {
                                // Open the user's profile
                                // For the current user, this will open his profile, but not in edit mode
                                // TODO : temporary until we update the way intents are handled in profile activity
                                val intent = Intent(context, ProfileActivity::class.java)
                                intent.putExtra("isViewingCoach", true)
                                intent.putExtra("email", it.email)
                                context.startActivity(intent)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 60.dp)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "${groupEvent.participants.size}/${groupEvent.maxParticipants} participants",
                            style = MaterialTheme.typography.body1,
                            color = Color.Gray
                        )
                    }
                }
            },
        )
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary
        ) {
            tabs.forEachIndexed { index, tabItem ->
                Tab(
                    modifier = Modifier.testTag(tabItem.tag),
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = Color.Gray,
                    text = { Text(tabItem.title) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                tabs[selectedTabIndex].content()
                Spacer(modifier = Modifier.height(70.dp))
            }
            if (currentUser.email == groupEvent.organizer || currentUser.email in groupEvent.participants) {
                DisablableExtendFloatingActionButton(
                    modifier = Modifier
                        .testTag(CHAT)
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    icon = Icons.Filled.Chat,
                    contentDescription = "Go to event chat",
                    text = { Text("CHAT") },
                    onClick = {
                        // Open the chat activity
                        val intent = Intent(context, ChatActivity::class.java)
                        intent.putExtra("chatId", groupEvent.groupEventId)
                        context.startActivity(intent)
                    }
                )
            } else {
                DisablableExtendFloatingActionButton(
                    modifier = Modifier
                        .testTag(JOIN_EVENT)
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    icon = if (groupEvent.participants.size < groupEvent.maxParticipants) Icons.Filled.RocketLaunch else null,
                    contentDescription = "Join event",
                    text = {
                        if (groupEvent.participants.size < groupEvent.maxParticipants)
                            Text("JOIN EVENT")
                        else
                            Text("THIS EVENT IS FULLY BOOKED")
                    },
                    onClick = onJoinEventClick,
                    enabled = groupEvent.participants.size < groupEvent.maxParticipants
                )
            }
        }
    }
}

/**
 * Composable that displays the date of an event.
 */
@Composable
private fun DayBox(
    dayOfMonth: Int,
    month: Month
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.primary)
            .defaultMinSize(60.dp, 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.testTag(EVENT_MONTH),
            text = month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.US).uppercase(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            modifier = Modifier.testTag(EVENT_DAY),
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

/**
 * Composable that displays a row with an icon and a text, used for the event details.
 */
@Composable
private fun IconTextRow(
    icon: ImageVector,
    contentDescription: String,
    text: String,
    tag: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(25.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(7.dp))
        if (onClick != null) {
            ClickableText(
                modifier = Modifier.testTag(tag),
                text = AnnotatedString(text),
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                onClick = { onClick() }
            )
        } else {
            Text(
                modifier = Modifier.testTag(tag),
                text = text,
                style = MaterialTheme.typography.body1,
                color = Color.Gray
            )
        }
    }
}

/**
 * Composable that displays a row with a user's profile picture and name.
 */
@Composable
fun SmallUserInfoListItem(
    userInfo: UserInfo,
    onClick: (() -> Unit)? = null
) {
    SmallListItem(
        image = ImageData(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "${userInfo.firstName} ${userInfo.lastName}'s profile picture"
        ),
        title = "${userInfo.firstName} ${userInfo.lastName}",
        onClick = onClick
    )
}

// Needed because of https://stackoverflow.com/questions/68847231/jetpack-compose-how-to-disable-floatingaction-button
/**
 * A [FloatingActionButton] that can be disabled.
 */
@Composable
fun DisablableExtendFloatingActionButton(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit),
    enabled: Boolean = true
) {
    Button(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        shape = CircleShape,
        onClick = onClick,
        enabled = enabled,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp,
            hoveredElevation = 8.dp,
            focusedElevation = 8.dp,
            disabledElevation = 6.dp
        )
    ) {
        icon?.let {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
        }
        text()
        icon?.let {
            Spacer(Modifier.size(2.dp))
        }
    }
}