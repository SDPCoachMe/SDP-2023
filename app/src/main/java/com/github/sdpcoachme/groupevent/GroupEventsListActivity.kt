package com.github.sdpcoachme.groupevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventsListActivity.TestTags.Tabs.Companion.ALL
import com.github.sdpcoachme.groupevent.GroupEventsListActivity.TestTags.Tabs.Companion.MY_EVENTS
import com.github.sdpcoachme.ui.*
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.DarkOrange
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

class GroupEventsListActivity : ComponentActivity() {

    class TestTags {
        class Tabs {
            companion object {
                const val ALL = "all"
                const val MY_EVENTS = "myEvents"
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



        setContent {

            // TODO: sort by date
            // TODO: in the "all" tab, do not show events that are already over
            var myEvents by remember { mutableStateOf<List<GroupEvent>>(emptyList()) }
            var allEvents by remember { mutableStateOf<List<GroupEvent>>(emptyList()) }

            data class TabItem(
                val title: String,
                val tag: String
            )
            val tabs = listOf(
                TabItem("ALL", ALL),
                TabItem("MY EVENTS", MY_EVENTS)
            )
            var selectedTabIndex by remember { mutableStateOf(0) }
            val selectedTab = tabs[selectedTabIndex]

            val displayedGroupEvents = when (selectedTab.tag) {
                ALL ->
                    allEvents
                MY_EVENTS ->
                    myEvents
                else ->
                    throw IllegalStateException("Unknown tab ${selectedTab.tag}")
            }

            CoachMeTheme {
                Dashboard(
                    title = "Group events",
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex
                    ) {
                        tabs.forEachIndexed { index, tabItem ->
                            Tab(
                                modifier = Modifier.testTag(tabItem.tag),
                                text = { Text(tabItem.title) },
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index }
                            )
                        }
                    }
                    LazyColumn {
                        items(displayedGroupEvents) { groupEvent ->
                            GroupEventItem(
                                groupEvent = groupEvent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupEventItem(
    groupEvent: GroupEvent
) {
    val context = LocalContext.current

    val eventStart = LocalDateTime.parse(groupEvent.event.start)
    val eventEnd = LocalDateTime.parse(groupEvent.event.end)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    ListItem(
        title = groupEvent.event.name,
        firstRow = {
            IconTextRow(
                icon = IconData(
                    icon = groupEvent.event.sport.sportIcon,
                    contentDescription = "Event sport"
                ),
                text = groupEvent.event.sport.sportName
            )
        },
        secondRow = {
            IconTextRow(
                icon = IconData(
                    icon = Icons.Default.Place,
                    contentDescription = "Event location"
                ),
                text = groupEvent.event.address.name
            )
        },
        secondColumn = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = eventStart.format(dateFormatter),
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${eventStart.format(timeFormatter)}–${eventEnd.format(timeFormatter)}",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            if (eventStart.isBefore(LocalDateTime.now())) {
                Label(
                    text = "PAST EVENT",
                    icon = IconData(
                        icon = Icons.Default.History,
                        contentDescription = "Event already took place"
                    ),
                    backgroundColor = DarkOrange,
                    contentColor = Color.White
                )
            } else if (groupEvent.participants.size >= groupEvent.maxParticipants) {
                Label(
                    text = "FULLY BOOKED",
                    icon = IconData(
                        icon = Icons.Default.EventBusy,
                        contentDescription = "Event is fully booked"
                    ),
                    backgroundColor = DarkOrange,
                    contentColor = Color.White
                )
            }
        },
        firstColumnMaxWidth = 0.6f,
        onClick = {
            // Open event details activity
            val intent = GroupEventDetailsActivity.getIntent(context, groupEvent.groupEventId)
            context.startActivity(intent)
        }
    )
}

// TODO remove
@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    GroupEventItem(
        GroupEvent(
            event = Event(
                name = "My first event has a very very long name that should be truncated",
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
            maxParticipants = 2,
            participants = listOf("jammy@email.com", "lolo@email.com")
        )
    )
}