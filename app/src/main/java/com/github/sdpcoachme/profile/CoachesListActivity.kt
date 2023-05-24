package com.github.sdpcoachme.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Place
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
import com.github.sdpcoachme.ui.ListItem
import kotlinx.coroutines.future.await
import java.util.Collections
import java.util.concurrent.CompletableFuture


class CoachesListActivity : ComponentActivity() {
    class TestTags {
        class Buttons {
            companion object {
                const val FILTER = "filterSearch"
            }
        }
    }

    private lateinit var store: CachingStore
    private lateinit var emailFuture: CompletableFuture<String>

    // Allows to notice testing framework that the activity is ready
    var stateLoading = CompletableFuture<Void>()

    // Observable state of the current sports used to filter the coaches list
    private lateinit var selectSportsHandler: (Intent) -> CompletableFuture<List<Sports>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isViewingContacts = intent.getBooleanExtra("isViewingContacts", false)
        store = (application as CoachMeApplication).store

        emailFuture = store.getCurrentEmail()

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
            LaunchedEffect(true) {
                email = emailFuture.await()
                if (isViewingContacts) {
                    contactRowInfos = store
                        .getContactRowInfo(email = email)
                        .await()
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
                stateLoading.complete(null)
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
                        // Filtering should not influence the coaches list in contacts view
                        // We still show user with no favourite sports, especially for testing purposes
                        if (user.sports.isEmpty()
                            || !Collections.disjoint(user.sports, sportsFilter)) {
                            UserInfoListItem(currentUserEmail = currentUserEmail, user = user, isViewingContacts = false)
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
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                ) {
                    Icon(
                        imageVector = Default.Tune,
                        contentDescription = "Filter nearby coaches by sports"
                    )
                }
            }
        }
    }

    /**
     * Displays a single user info in a list, or a chat preview if isViewingContacts is true.
     */
    @Composable
    fun UserInfoListItem(currentUserEmail: String, user: UserInfo = UserInfo(), isViewingContacts: Boolean = false, contactRowInfo: ContactRowInfo = ContactRowInfo()) {
        val context = LocalContext.current

        if (isViewingContacts) {
            ListItem(
                image = ImageData(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
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
            ListItem(
                image = ImageData(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
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
                onClick = {
                    /*class CoachesListCallback : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            val intent = Intent(this@CoachesListActivity, MapActivity::class.java)
                            ContextCompat.startActivity(this@CoachesListActivity, intent, null)
                        }
                    }
                    val callback = CoachesListCallback()
                    onBackPressedDispatcher.addCallback(this, callback)*/

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
}



