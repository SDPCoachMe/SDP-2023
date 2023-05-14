package com.github.sdpcoachme.groupevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class GroupEventDetailsActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val TITLE = "title"
        }
        class Buttons {
            companion object {
                const val BACK = "back"
            }
        }
    }

    private lateinit var store : CachingStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store

        // do not allow the activity to launch without an id. (double bang)
        val groupEventId = intent.getStringExtra("groupEventId")!!

        // TODO: get the group event from the store
        val futureGroupEvent = CompletableFuture.completedFuture(
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
                    description = "I want to invite you to my birthday party!" +
                            "\nPlease make sure to bring some food and drinks." +
                            "\nI will provide some snacks and drinks as well."
                ),
                organiser = "bry.gotti@outlook.com",
                maxParticipants = 20,
                participants = listOf("damian.kopp01@gmail.com", "ennassih.yann@gmail.com")
            )
        )

        setContent {
            CoachMeTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Event details", modifier = Modifier.testTag(TITLE))
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }, modifier = Modifier.testTag(BACK)) {
                                    Icon(Icons.Filled.ArrowBack, "Back")
                                }
                            }
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

                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        Greeting("Android")
    }
}