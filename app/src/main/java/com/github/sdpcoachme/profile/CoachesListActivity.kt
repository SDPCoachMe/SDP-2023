package com.github.sdpcoachme.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.CAMPUS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.CoachesListActivity.TestTags.Buttons.Companion.FILTER
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.Purple200
import kotlinx.coroutines.future.await
import java.util.*
import java.util.concurrent.CompletableFuture

class CoachesListActivity : ComponentActivity() {

    class TestTags {
        class Buttons {
            companion object {
                const val FILTER = "filterSearch"
            }
        }
    }

    // Allows to notice testing framework that the activity is ready

    private lateinit var store: CachingStore
    private lateinit var emailFuture: CompletableFuture<String>


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

        val futureListOfCoaches = emailFuture.thenCompose { email ->
            if (isViewingContacts) {
                store.getChatContacts(email = email)
            } else {
                store.getAllUsersByNearest(
                    latitude = userLatLng.latitude,
                    longitude = userLatLng.longitude
                ).thenApply {
                    it.filter { user -> user.coach }
                }
            }
        }
        setContent {
            var listOfCoaches by remember { mutableStateOf(listOf<UserInfo>()) }

            // Proper way to handle result of a future in a Composable.
            // This makes sure the listOfCoaches state is updated only ONCE, when the future is complete
            // This is because the code in LaunchedEffect(true) will only be executed once, when the
            // Composable is first created (given that the parameter key1 never changes). The code won't
            // be executed on every recomposition.
            // See https://developer.android.com/jetpack/compose/side-effects#rememberupdatedstate
            LaunchedEffect(true) {
                listOfCoaches = futureListOfCoaches.await()

                // Activity is now ready for testing
                stateLoading.complete(null)
            }

            val title = if (isViewingContacts) stringResource(R.string.contacts)
            else stringResource(R.string.title_activity_coaches_list)
            CoachMeTheme {
                Dashboard(title) {
                    CoachesList(it, listOfCoaches, isViewingContacts)
                }
            }
        }
    }

    /**
     * Displays a list of nearby coaches or messaging contacts.
     */
    @Composable
    fun CoachesList(
        modifier: Modifier,
        listOfCoaches: List<UserInfo>,
        isViewingContacts: Boolean
    ) {
        val context = LocalContext.current
        // initially all sports are selected
        var sportsFilter by remember { mutableStateOf(Sports.values().toList()) }

        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn {
                items(listOfCoaches) {user ->
                    // Filtering should not influence the coaches list in contacts view
                    // We still show user with no favourite sports, especially for testing purposes
                    if (user.sports.isEmpty() || !Collections.disjoint(user.sports, sportsFilter)) {
                        UserInfoListItem(user, isViewingContacts)
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
                    backgroundColor = Purple200
                ) {
                    Icon(
                        imageVector = Default.Tune,
                        contentDescription = "Filter nearby coaches by sports"
                    )
                }
            }
        }
    }

    // TODO: make this composable use new ListItem composable in package ui
    @Composable
    fun UserInfoListItem(user: UserInfo, isViewingContacts: Boolean) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isViewingContacts) {
                        val displayChatIntent = Intent(context, ChatActivity::class.java)
                        displayChatIntent.putExtra("toUserEmail", user.email)
                        context.startActivity(displayChatIntent)
                    } else {
                        val displayCoachIntent = Intent(context, ProfileActivity::class.java)
                        displayCoachIntent.putExtra("email", user.email)
                        displayCoachIntent.putExtra("isViewingCoach", true)
                        context.startActivity(displayCoachIntent)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: might be a good idea to merge the profile picture code used here and the one used in EditProfileActivity
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "${user.firstName} ${user.lastName}'s profile picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Default.Place,
                        tint = Color.Gray,
                        contentDescription = "${user.firstName} ${user.lastName}'s location",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Temporary, until we implement proper location handling
                    Text(
                        text = user.address.name,
                        color = Color.Gray,
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    user.sports.map {
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
        }
        Divider()
    }
}