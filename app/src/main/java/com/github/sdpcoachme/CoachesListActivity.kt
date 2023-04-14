package com.github.sdpcoachme

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

class CoachesListActivity : ComponentActivity() {
    var stateLoading = CompletableFuture<Void>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: update this to be current device location
        val currentLat = 46.519054480712015
        val currentLong = 6.566757578464391
        val futureListOfCoaches = (application as CoachMeApplication).database
            .getAllUsersByNearest(
                latitude = currentLat,
                longitude = currentLong
            ).thenApply {
                it.filter { user -> user.coach }
            }

        setContent {
            var listOfCoaches by remember { mutableStateOf(listOf<UserInfo>()) }

            LaunchedEffect(true) {
                listOfCoaches = futureListOfCoaches.await()
                stateLoading.complete(null)
            }

            CoachMeTheme {
                LazyColumn {
                    items(listOfCoaches) { user ->
                        UserInfoListItem(user)
                    }
                }
            }
        }
    }
}

@Composable
fun UserInfoListItem(user: UserInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val displayCoachIntent = Intent(context, ProfileActivity::class.java)
                displayCoachIntent.putExtra("email", user.email)
                displayCoachIntent.putExtra("isViewingCoach", true)
                context.startActivity(displayCoachIntent)
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