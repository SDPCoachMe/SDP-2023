package com.github.sdpcoachme.groupevent

import android.os.Bundle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_DESCRIPTION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_LOCATION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_SPORT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_TIME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.ORGANIZER_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Tabs.Companion.ABOUT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Tabs.Companion.PARTICIPANTS
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

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
        }

        class Buttons {
            companion object {
                const val BACK = "back"
            }
        }

        class Tabs {
            companion object {
                const val ABOUT = "about"
                const val PARTICIPANTS = "participants"
            }
        }
    }

    // To notify tests that the activity is ready
    lateinit var stateLoading: CompletableFuture<Void>

    private lateinit var store: CachingStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store
        stateLoading = CompletableFuture()

        // do not allow the activity to launch without an id. (double bang)
        val groupEventId = intent.getStringExtra("groupEventId")!!

        val futureGroupEvent = store.getGroupEvent(groupEventId)

        setContent {

            var groupEvent by remember { mutableStateOf<GroupEvent?>(null) }
            var organizer by remember { mutableStateOf<UserInfo?>(null) }
            var participants by remember { mutableStateOf<List<UserInfo>?>(null) }
            var currentUser by remember { mutableStateOf<UserInfo?>(null) }

            LaunchedEffect(true) {
                groupEvent = futureGroupEvent.await()
                organizer = store.getUser(groupEvent!!.organiser).await()
                participants = groupEvent!!.participants.map {
                    store.getUser(it).await()
                }
                currentUser = store.getUser(store.getCurrentEmail().await()).await()
                stateLoading.complete(null)
            }

            CoachMeTheme {
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
                            // TODO
                            //                            actions = {
                            //                                IconButton(
                            //                                    onClick = {
                            //                                        onSubmit(value)
                            //                                    },
                            //                                    modifier = Modifier.testTag(EditTextActivity.TestTags.Companion.Buttons.DONE)
                            //                                ) {
                            //                                    Icon(
                            //                                        Icons.Filled.Done, "Done",
                            //                                        tint = MaterialTheme.colors.onPrimary)
                            //                                }
                            //                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.padding(padding)
                    ) {
                        if (groupEvent != null && currentUser != null
                            && organizer != null && participants != null) {
                            GroupEventDetailsLayout(
                                groupEvent!!,
                                organizer!!,
                                currentUser!!,
                                participants!!
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * This is the main layout for the GroupEventDetailsActivity
 *
 * @param groupEvent the GroupEvent to display
 */
@Composable
fun GroupEventDetailsLayout(
    groupEvent: GroupEvent,
    organizer: UserInfo,
    currentUser: UserInfo,
    participants: List<UserInfo>
) {
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
                    style = MaterialTheme.typography.body1,
                    onClick = {
                        // TODO : open organizer profile
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
                    // TODO : open map
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
                    participants.filter { it.email != currentUser.email }
                        .map {
                            SmallUserInfoListItem(
                                userInfo = it,
                                onClick = {
                                    // TODO : open user profile
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
            if (currentUser.email in groupEvent.participants) {
                DisablableExtendFloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    icon = Icons.Filled.Chat,
                    contentDescription = "Go to event chat",
                    text = { Text("CHAT") },
                    onClick = {
                        // TODO : go to event chat
                    }
                )
            } else {
                DisablableExtendFloatingActionButton(
                    modifier = Modifier
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
                    onClick = {
                        // TODO : join event
                    },
                    enabled = groupEvent.participants.size < groupEvent.maxParticipants
                )
            }
        }
    }
}

// Needed because of https://stackoverflow.com/questions/68847231/jetpack-compose-how-to-disable-floatingaction-button
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

@Composable
fun DayBox(
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
            text = month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.US).uppercase(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
fun IconTextRow(
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
                style = MaterialTheme.typography.body1,
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

// TODO: might be a way to modularize with UserInfoListItem from CoachesListActivity
@Composable
fun SmallUserInfoListItem(
    userInfo: UserInfo,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
            .padding(10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "${userInfo.firstName} ${userInfo.lastName}'s profile picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .padding(0.dp, 0.dp, 0.dp, 0.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${userInfo.firstName} ${userInfo.lastName}",
            style = MaterialTheme.typography.body1
        )
    }
    Divider()
}

data class TabItem(
    val title: String,
    val tag: String,
    val content: @Composable () -> Unit
)

// TODO: remove this
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        GroupEventDetailsLayout(
            GroupEvent(
                event = Event(
                    name = "My first event",
                    color = EventColors.ORANGE.color.value.toString(),
                    start = "2023-05-19T13:00",
                    end = "2023-05-19T15:00",
                    sport = Sports.RUNNING,
                    address = Address(
                        placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
                        name = "Lausanne, Switzerland",
                        latitude = 46.5196535,
                        longitude = 6.6335972
                    ),
                    description = "Lorem ipsum dolor sit amet. Ab fugit eveniet ut ipsam tenetur sed iure illum vel nemo maxime. Non ullam harum non obcaecati odio a voluptate facilis ex internos galisum non placeat sunt ad quaerat nobis aut maiores molestiae.\n" +
                            "\n" +
                            "Cum sunt nihil non aliquid fugiat aut exercitationem ullam et ipsum explicabo et officia provident quo accusantium ipsa ea voluptas dolorem. Ex officia culpa a aliquam nostrum sed exercitationem velit et praesentium exercitationem.\n" +
                            "\n" +
                            "Id rerum quam aut iusto ipsam aut repudiandae debitis. Est ullam libero in quasi nisi ut esse voluptatem ab quas ipsam et repellat voluptatum et incidunt odit et aliquam ipsam. Aut fugit laudantium in dolores similique ea rerum dolore.\n" +
                            "\n" +
                            "Quo aperiam esse cum natus iure et iure magnam! Sit voluptate quis 33 architecto laudantium nam dolorum officia aut consequatur quaerat quo odio voluptatem et numquam velit eos fugiat odio. Est dolorum quas ut consequuntur nostrum in velit quam eos omnis galisum sed nulla quod?\n" +
                            "\n" +
                            "Non soluta nisi sit voluptate quaerat et praesentium doloribus quo aspernatur ipsa aut incidunt atque At dolorem inventore. Qui ipsam commodi ea accusamus aliquid et atque unde et dolor blanditiis id internos sunt? Id excepturi illo qui praesentium similique qui velit tempora? 33 molestiae omnis et commodi optio aut deserunt distinctio 33 itaque magni.\n" +
                            "\n" +
                            "Qui deleniti tempore non neque asperiores aut minima laboriosam et amet fugiat qui voluptatum dolores aut sequi enim vel earum consequatur. Et ratione neque ad commodi mollitia non doloremque animi qui veritatis optio. Eum repudiandae eveniet et accusantium laborum vel assumenda magni sed esse deserunt sed veniam vero non minima ullam quo ratione corrupti?\n" +
                            "\n" +
                            "Ad excepturi incidunt ut iste blanditiis aut dolore itaque id consequatur magni hic laudantium facilis sed sint minima non consequuntur totam. Ut error internos non minima velit ex rerum ratione sit suscipit aliquid eos consectetur quia ea repellat dolor. At accusamus sapiente ad numquam iure nam tenetur accusamus ea excepturi blanditiis est aliquid quos sit maiores galisum.\n" +
                            "\n" +
                            "Non nemo consequatur in galisum nobis aut mollitia laboriosam sed rerum accusantium! Et quia maiores et enim explicabo a ipsa dicta et veniam incidunt aut accusantium omnis aut facere ratione eum quas minus.\n" +
                            "\n" +
                            "Qui voluptatum fuga et necessitatibus illum non suscipit cupiditate et rerum rerum 33 voluptatem porro. Vel voluptas magnam in officia voluptatem non reprehenderit reprehenderit est quia aperiam aut magni dicta. Eos asperiores nostrum non beatae ipsum quo soluta quod eos sunt illum ad voluptate cupiditate vel provident corporis. At asperiores culpa quo voluptatem voluptatem qui architecto minus et excepturi fuga et dolorem laboriosam.\n" +
                            "\n" +
                            "Quo nulla libero id animi incidunt et fuga eius sed nihil earum a quam magni. Et quibusdam commodi 33 unde distinctio id voluptas laborum vel eveniet dolores est commodi doloremque. Sed praesentium voluptas ad doloribus nihil a vitae adipisci quo inventore quaerat ut omnis natus est quisquam veniam non repudiandae impedit."
                ),
                organiser = "bry.gotti@outlook.com",
                maxParticipants = 20,
                participants = listOf("jammy@email.com", "lolo@email.com")
            ),
            UserInfo(
                firstName = "Bryan",
                lastName = "Gotti",
                email = "bry.gotti@outlook.com",
                address = Address(
                    placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
                    name = "Lausanne, Switzerland",
                    latitude = 46.5196535,
                    longitude = 6.6335972
                ),
                phone = "0123456789",
                sports = listOf(Sports.SKI, Sports.SWIMMING),
                coach = true
            ),
            UserInfo(
                firstName = "Michel",
                lastName = "Sardoux",
                email = "michou@email.com",
                address = Address(
                    placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
                    name = "Lausanne, Switzerland",
                    latitude = 46.5196535,
                    longitude = 6.6335972
                ),
                phone = "0123456789",
                sports = listOf(Sports.SKI, Sports.SWIMMING),
                coach = false
            ),
            listOf(
                UserInfo(
                    firstName = "James",
                    lastName = "Dolorian",
                    email = "jammy@email.com",
                    address = Address(
                        placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
                        name = "Lausanne, Switzerland",
                        latitude = 46.5196535,
                        longitude = 6.6335972
                    ),
                    phone = "0123456789",
                    sports = listOf(Sports.SKI, Sports.SWIMMING),
                    coach = false
                ),
                UserInfo(
                    firstName = "Loris",
                    lastName = "Pinaclio",
                    email = "lolo@email.com",
                    address = Address(
                        placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
                        name = "Lausanne, Switzerland",
                        latitude = 46.5196535,
                        longitude = 6.6335972
                    ),
                    phone = "0123456789",
                    sports = listOf(Sports.TENNIS),
                    coach = false
                )
            )

        )
    }
}