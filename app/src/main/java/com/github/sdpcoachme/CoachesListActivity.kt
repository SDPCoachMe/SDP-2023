package com.github.sdpcoachme

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class CoachesListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val futureListOfCoaches = (application as CoachMeApplication).database
            .getAllUsers().thenApply {
                it.filter { user -> user.coach }
            }
        setContent {
            var listOfCoaches by remember { mutableStateOf(listOf<UserInfo>()) }

            // TODO: Need to handle the future correctly in cases like this (might need to use coroutines)
            var f by remember { mutableStateOf(futureListOfCoaches) }
            f.thenAccept {
                listOfCoaches = it
                f = CompletableFuture.completedFuture(null)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: open user profile in details
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(100.dp)
            ,
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
                    text = user.location,
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