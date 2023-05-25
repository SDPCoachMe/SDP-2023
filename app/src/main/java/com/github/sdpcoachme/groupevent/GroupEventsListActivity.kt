package com.github.sdpcoachme.groupevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventsListActivity.TestTags.Tabs.Companion.ALL
import com.github.sdpcoachme.groupevent.GroupEventsListActivity.TestTags.Tabs.Companion.MY_EVENTS
import com.github.sdpcoachme.ui.*
import com.github.sdpcoachme.ui.theme.label
import com.github.sdpcoachme.ui.theme.onLabel
import kotlinx.coroutines.future.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

/**
 * The activity that displays the list of group events.
 */
class GroupEventsListActivity : ComponentActivity() {

    class TestTags {

        data class GroupEventItemTags(val groupEvent: GroupEvent) {
            val TITLE = "groupEventItemTitle-${groupEvent.groupEventId}"
            val DATE = "groupEventItemDate-${groupEvent.groupEventId}"
            val TIME = "groupEventItemTime-${groupEvent.groupEventId}"
            val SPORT = "groupEventItemSport-${groupEvent.groupEventId}"
            val LOCATION = "groupEventItemLocation-${groupEvent.groupEventId}"
            val FULLY_BOOKED = "groupEventItemFullyBooked-${groupEvent.groupEventId}"
            val PAST_EVENT = "groupEventItemPastEvent-${groupEvent.groupEventId}"
        }

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
            var myEvents by remember { mutableStateOf<List<GroupEvent>>(emptyList()) }
            var allEvents by remember { mutableStateOf<List<GroupEvent>>(emptyList()) }

            LaunchedEffect(true) {
                allEvents = store.getUpcomingGroupEventsByDate().await()
                val email = store.getCurrentEmail().await()
                myEvents = store.getGroupEventsOfUserByDate(email).await().reversed() // To have the oldest last
                stateLoading.complete(null)
            }

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

            Dashboard(
                title = "Group events",
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        backgroundColor = MaterialTheme.colors.background,
                        contentColor = MaterialTheme.colors.primary
                    ) {
                        tabs.forEachIndexed { index, tabItem ->
                            Tab(
                                modifier = Modifier.testTag(tabItem.tag),
                                text = { Text(tabItem.title) },
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                selectedContentColor = MaterialTheme.colors.primary,
                                unselectedContentColor = Color.Gray,
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

/**
 * A group event item in the list of group events.
 */
@Composable
fun GroupEventItem(
    groupEvent: GroupEvent,
) {
    val context = LocalContext.current
    val tags = GroupEventsListActivity.TestTags.GroupEventItemTags(groupEvent)
    val eventStart = LocalDateTime.parse(groupEvent.event.start)
    val eventEnd = LocalDateTime.parse(groupEvent.event.end)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    ListItem(
        title = groupEvent.event.name,
        titleTag = tags.TITLE,
        firstRow = {
            IconTextRow(
                icon = IconData(
                    icon = groupEvent.event.sport.sportIcon,
                    contentDescription = "Event sport"
                ),
                text = groupEvent.event.sport.sportName,
                textTag = tags.SPORT
            )
        },
        secondRow = {
            IconTextRow(
                icon = IconData(
                    icon = Icons.Default.Place,
                    contentDescription = "Event location"
                ),
                text = groupEvent.event.address.name,
                textTag = tags.LOCATION
            )
        },
        secondColumn = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.testTag(tags.DATE),
                    text = eventStart.format(dateFormatter),
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.testTag(tags.TIME),
                    text = "${eventStart.format(timeFormatter)}–${eventEnd.format(timeFormatter)}",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            if (eventStart.isBefore(LocalDateTime.now())) {
                Label(
                    text = "PAST EVENT",
                    textTag = tags.PAST_EVENT,
                    icon = IconData(
                        icon = Icons.Default.History,
                        contentDescription = "Event already took place"
                    ),
                    backgroundColor = MaterialTheme.colors.label,
                    contentColor = MaterialTheme.colors.onLabel
                )
            } else if (groupEvent.participants.size >= groupEvent.maxParticipants) {
                Label(
                    text = "FULLY BOOKED",
                    textTag = tags.FULLY_BOOKED,
                    icon = IconData(
                        icon = Icons.Default.EventBusy,
                        contentDescription = "Event is fully booked"
                    ),
                    backgroundColor = MaterialTheme.colors.label,
                    contentColor = MaterialTheme.colors.onLabel
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