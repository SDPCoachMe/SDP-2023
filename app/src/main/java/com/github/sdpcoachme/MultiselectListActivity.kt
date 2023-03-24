package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.ListItem
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.ui.theme.CoachMeTheme

class MultiselectListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as CoachMeApplication).database
        // todo handle the null better here
        val email = intent.getStringExtra("email")?: "no valid email"
        val sportNames = Sports.values().map { it.sportName }
        val context = applicationContext
        val selectedSports = mutableListOf<Sports>()
        setContent {
            CoachMeTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select your favorite sports:",

                    )
                    MultiSelectList(
                        sportNames,
                        toggleSelectSport = { sport ->
                            if (selectedSports.contains(sport)) {
                                selectedSports.remove(sport)
                            } else {
                                selectedSports.add(sport)
                            }
                        })
                    Button(
                        //todo add test tag
                        //modifier = Modifier.testTag(SignupActivity.TestTags.Buttons.SIGN_UP),
                        onClick = {
                            database.getUser(email)
                                .thenApply { user ->
                                    user.copy(sports = selectedSports.toList())
                                }
                                .thenApply { user ->
                                    database.addUser(user)
                                }.handle { _, exception ->
                                    when (exception) {
                                        null -> {
                                            val intent = Intent(context, DashboardActivity::class.java)
                                            intent.putExtra("email", email)
                                            startActivity(intent)
                                        }
                                        else -> {
                                            // TODO handle the exception
                                        }
                                    }
                                }
                        }
                    )
                    { Text("REGISTER") }

                }
            }

        }
    }

    @Composable
    fun MultiSelectList(items: List<String>, toggleSelectSport: (Sports) -> Unit) {
        var sportItems by remember {
            mutableStateOf(
                items.map {
                    ListItem(
                        title = it,
                        selected = false
                    )
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sportItems.size) { i ->
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sportItems = sportItems.mapIndexed { j, item ->
                                if (i == j) {
                                    item.copy(selected = !item.selected)
                                } else {
                                    item
                                }
                            }
                            toggleSelectSport(Sports.values()[i])
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = sportItems[i].title)
                    if (sportItems[i].selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}