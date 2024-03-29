package com.github.sdpcoachme.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.CAMPUS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.CoachesListActivity.TestTags.Buttons.Companion.FILTER
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.IconData
import com.github.sdpcoachme.ui.IconTextRow
import com.github.sdpcoachme.ui.IconsRow
import com.github.sdpcoachme.ui.ImageData
import com.github.sdpcoachme.ui.Label
import com.github.sdpcoachme.ui.ListItem
import com.github.sdpcoachme.ui.theme.onRating
import com.github.sdpcoachme.ui.theme.rating
import kotlinx.coroutines.future.await
import java.util.Collections
import java.util.concurrent.CompletableFuture

class CoachesListActivity : ComponentActivity() {

    class TestTags {
        data class CoachesListTags(val user: UserInfo) {
            val RATING = "coachRating-${user.email}"
        }


        class Buttons {
            companion object {
                const val FILTER = "filterSearch"
            }
        }
    }

    // Allows to notice testing framework that the activity is ready
    var stateUpdated = CompletableFuture<Void>()
    // To refresh the list of events, when we come back to this activity
    var refreshState by mutableStateOf(false)

    private lateinit var store: CachingStore

    // Observable state of the current sports used to filter the coaches list
    private lateinit var selectSportsHandler: (Intent) -> CompletableFuture<List<Sports>>

    override fun onResume() {
        super.onResume()
        // Refresh the list of events by triggering launched effect
        refreshState = !refreshState
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("openChat", false)) {
            val chatId = intent.getStringExtra("chatId")!!
            val email = intent.getStringExtra("pushNotification_currentUserEmail")!!
            val chatIntent = Intent(this, ChatActivity::class.java)
                .putExtra("chatId", chatId)
                .putExtra("pushNotification_currentUserEmail", email)
            startActivity(chatIntent)
        }
        val isViewingContacts = intent.getBooleanExtra("isViewingContacts", false)
        store = (application as CoachMeApplication).store

        val locationProvider = (application as CoachMeApplication).locationProvider
        // Here we don't need the UserInfo
        locationProvider.updateContext(this, CompletableFuture.completedFuture(null))
        // the lastLocation return a null containing state if no location retrieval has been
        // performed yet. In production, this should for now never be the case as this code is
        // necessarily run after MapActivity. We keep it for robustness against tests.
        val userLatLng = locationProvider.getLastLocation().value?: CAMPUS

        selectSportsHandler = SelectSportsActivity.getHandler(this)

        setContent {
            var listOfCoaches by remember { mutableStateOf(listOf<UserInfo>()) }
            var contactRowInfos by remember { mutableStateOf(listOf<ContactRowInfo>()) }
            var email by remember { mutableStateOf("") }

            // Proper way to handle result of a future in a Composable.
            // This makes sure the listOfCoaches state is updated only ONCE, when the future is complete
            // This is because the code in LaunchedEffect(true) will only be executed once, when the
            // Composable is first created (given that the parameter key1 never changes). The code won't
            // be executed on every recomposition.
            // See https://developer.android.com/jetpack/compose/side-effects#rememberupdatedstate
            // Note: Now we trigger LaunchedEffect not once, but every time the refreshState changes.
            LaunchedEffect(refreshState) {
                email = store.getCurrentEmail()
                    .exceptionally {
                        // The following recovers from the user receiving a push notification, then logging out
                        // and then clicking on the notification. In this case, the intent will contain the email
                        val pushNotificationEmail = intent.getStringExtra("pushNotification_currentUserEmail")!!
                        store.setCurrentEmail(pushNotificationEmail)
                        pushNotificationEmail
                    }.await()

                if (isViewingContacts) {
                    // TODO: this is bad code and should be refactored
                    //  -> The participants should be fetched in the database method getContactRowInfo
                    //  But does it really make sense to use ContactRowInfo over Chat, then ?
                    //  (imo, ContactRowInfo and Chat should be merged into one class, and this should
                    //  be the only class used throughout the app)
                    val contactRowInfosTemp = store
                        .getContactRowInfo(email = email)
                        .await()
                    contactRowInfos = contactRowInfosTemp.map {
                        it.copy(participants = store.getChat(it.chatId).await().participants)
                    }

                } else {
                    listOfCoaches = store
                        .getAllUsersByNearest(
                            latitude = userLatLng.latitude,
                            longitude = userLatLng.longitude
                        ).thenApply {
                            it.filter { user -> user.coach }
                        }.await()
                }

                // Activity is now ready for testing
                stateUpdated.complete(null)
            }

            val title = if (isViewingContacts) stringResource(R.string.chats)
            else stringResource(R.string.title_activity_coaches_list)

            Dashboard(title) {
                CoachesList(it, email, listOfCoaches, isViewingContacts, contactRowInfos)
            }
        }
    }

    /**
     * Displays a list of nearby coaches or messaging contacts.
     */
    @Composable
    fun CoachesList(
        modifier: Modifier,
        currentUserEmail: String,
        listOfCoaches: List<UserInfo>,
        isViewingContacts: Boolean,
        contactRowInfos: List<ContactRowInfo>,
    ) {
        val context = LocalContext.current
        // initially all sports are selected
        var sportsFilter by remember { mutableStateOf(Sports.values().toList()) }

        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn {
                if (isViewingContacts) {
                    items(contactRowInfos) { contactRowInfo ->
                        UserInfoListItem(currentUserEmail = currentUserEmail, isViewingContacts = true, contactRowInfo = contactRowInfo)
                    }
                } else {
                    items(listOfCoaches) { user ->
                        var rating by remember { mutableStateOf(0) }

                        LaunchedEffect(true) {
                            rating = store.getCoachAverageRating(user.email).await()
                        }

                        // Filtering should not influence the coaches list in contacts view
                        // We still show user with no favourite sports, especially for testing purposes
                        if (user.sports.isEmpty()
                            || !Collections.disjoint(user.sports, sportsFilter)) {
                            UserInfoListItem(currentUserEmail = currentUserEmail, user = user,
                                isViewingContacts = false, coachRating = rating)
                        }
                    }
                }
            }
            if (!isViewingContacts) {
                // Button to add a sport filter for the shown coaches list.
                FloatingActionButton(
                    onClick = {
                        // Updates the sportsFilter state with the result of the SelectSportsActivity
                        // which relaunches the CoachesList composable on state change.
                        selectSportsHandler(
                            SelectSportsActivity.getIntent(
                                context = context,
                                title = "Filter coaches by sport",
                                initialValue = sportsFilter
                            )).thenApply {sportsFilter = it }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag(FILTER),
                    backgroundColor = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Icon(
                        imageVector = Default.Tune,
                        contentDescription = "Filter nearby coaches by sports"
                    )
                }
            }
        }
    }

}


/**
 * Displays a single user info in a list, or a chat preview if isViewingContacts is true.
 */
@Composable
fun UserInfoListItem(currentUserEmail: String, user: UserInfo = UserInfo(), isViewingContacts: Boolean = false,
                     contactRowInfo: ContactRowInfo = ContactRowInfo(), coachRating: Int = 0) {
    val context = LocalContext.current

    if (isViewingContacts) {
        val picture = if (contactRowInfo.isGroupChat) {
            GroupEvent.getPictureResource(contactRowInfo.chatId)
        } else {
            // Make sure we handle the case where participants is empty (should never happen here though)
            // See the _TODO above in the LaunchedEffect and the one in ContactRowInfo for more details
            contactRowInfo.participants
                .firstOrNull { it != currentUserEmail }?.let { UserInfo.getPictureResource(it) } ?:
            UserInfo.getPictureResource("") // fallback to gray picture (displayed when
            // email is empty usually indicating loading state)
        }
        ListItem(
            image = ImageData(
                painter = painterResource(id = picture),
                contentDescription = contactRowInfo.chatTitle,
            ),
            title = contactRowInfo.chatTitle,
            firstRow = {
                val senderName = if (contactRowInfo.lastMessage.sender == currentUserEmail) "You" else contactRowInfo.lastMessage.senderName
                IconTextRow(
                    text = if (senderName.isNotEmpty()) "$senderName: ${contactRowInfo.lastMessage.content}" else "Tap to write a message",
                    maxLines = 2
                )
            },
            onClick = {
                val displayChatIntent = Intent(context, ChatActivity::class.java)
                displayChatIntent.putExtra("chatId", contactRowInfo.chatId)
                context.startActivity(displayChatIntent)
            }
        )
    } else {
        val tags = CoachesListActivity.TestTags.CoachesListTags(user)
        ListItem(
            image = ImageData(
                painter = painterResource(id = user.getPictureResource()),
                contentDescription = "${user.firstName} ${user.lastName}'s profile picture",
            ),
            title = "${user.firstName} ${user.lastName}",
            firstRow = {
                IconTextRow(
                    icon = IconData(icon = Default.Place, contentDescription = "${user.firstName} ${user.lastName}'s location"),
                    text = user.address.name
                )
            },
            secondRow = {
                IconsRow(icons = user.sports.map { sport ->
                    IconData(icon = sport.sportIcon, contentDescription = sport.sportName)
                })
            },
            secondColumn = {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(9.dp))
                    Label(
                        text = "$coachRating",
                        textTag = tags.RATING,
                        icon = IconData(
                            icon = Default.Star,
                            contentDescription = "Coach rating"
                        ),
                        backgroundColor = colors.rating,
                        contentColor = colors.onRating,
                        iconOnRight = true
                    )
                }
            },
            firstColumnMaxWidth = 0.7f,
            onClick = {
                val displayCoachIntent = Intent(context, ProfileActivity::class.java)
                displayCoachIntent.putExtra("email", user.email)
                if (user.email == currentUserEmail) {
                    displayCoachIntent.putExtra("isViewingCoach", false)
                } else {
                    displayCoachIntent.putExtra("isViewingCoach", true)
                }
                context.startActivity(displayCoachIntent)
            }
        )
    }
}



