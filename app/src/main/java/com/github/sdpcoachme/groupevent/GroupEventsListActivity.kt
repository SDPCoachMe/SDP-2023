package com.github.sdpcoachme.groupevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class GroupEventsListActivity : ComponentActivity() {

    class TestTags {
        companion object {
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
            CoachMeTheme {
                Dashboard(
                    title = "Group events",
                ) {
                    // TODO
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
}