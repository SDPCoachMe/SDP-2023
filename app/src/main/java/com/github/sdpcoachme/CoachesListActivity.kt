package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.Purple500
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

class CoachesListActivity : ComponentActivity() {
    // Allows to notice testing framework that the activity is ready
    var stateLoading = CompletableFuture<Void>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isViewingContacts = intent.getBooleanExtra("isViewingContacts", false)
        // TODO: update this to be current device location
        val currentLat = 46.519054480712015
        val currentLong = 6.566757578464391
        val database = (application as CoachMeApplication).database
        val futureListOfCoaches =
            if (isViewingContacts) {
                database.getChatContacts(email = database.getCurrentEmail())
            } else {
                database
                .getAllUsersByNearest(
                    latitude = currentLat,
                    longitude = currentLong
                ).thenApply {
                    it.filter { user -> user.coach }
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

            CoachMeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    TitleRow(isViewingContacts = isViewingContacts)

                    LazyColumn {
                        items(listOfCoaches) { user ->
                            UserInfoListItem(user, isViewingContacts)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TitleRow(isViewingContacts: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Purple500),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isViewingContacts) "Contacts" else "Nearby Coaches",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            color = Color.White
        )
    }
}

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
                    imageVector = Icons.Default.Place,
                    tint = Color.Gray,
                    contentDescription = "${user.firstName} ${user.lastName}'s location",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Temporary, until we implement proper location handling
                Text(
                    text = user.location.address,
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