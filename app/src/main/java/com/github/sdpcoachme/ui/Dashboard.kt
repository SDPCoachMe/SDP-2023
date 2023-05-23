package com.github.sdpcoachme.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.groupevent.GroupEventsListActivity
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.ScheduleActivity
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * Helper Dashboard class for TestTags of the dashboard UI
 */
class Dashboard {
    class TestTags {
        companion object {
            const val DRAWER_HEADER = "drawerHeader"
            const val DASHBOARD_EMAIL = "dashboardEmail"
            const val MENU_LIST = "menuList"
            const val BAR_TITLE = "barTitle"
        }
        class Buttons {
            companion object {
                const val HAMBURGER_MENU = "hamburgerMenu"
                const val PLAN = "plan"
                const val SCHEDULE = "schedule"
                const val PROFILE = "profile"
                const val COACHES_LIST = "coachesList"
                const val MESSAGING = "messaging"
                const val GROUP_EVENTS_LIST = "groupEventsList"
                const val LOGOUT = "logout"
            }
        }
    }
}

/**
 * Dashboard UI implemented as a left-sided drawer to navigate to other application activities.
 * @param appContent = set here the root composable of the current launched activity
 * @param title = title to display on the top application bar. It is a composable function that
 * takes a Modifier as parameter, and the modifier must be applied to the Text composable that
 * displays the title. This allows to add a test tag to the title, while still making it possible for
 * the caller to pass a custom composable as title.
 * @param UIDisplayed = future that will be completed when the UI is displayed
 */
@Composable
fun Dashboard(title: @Composable (Modifier) -> Unit,
                UIDisplayed: CompletableFuture<Void> = CompletableFuture<Void>(),
                appContent: @Composable (Modifier) -> Unit) {

    val context = LocalContext.current
    // equivalent to remember { ScaffoldState(...) }
    val hamburgerState = rememberScaffoldState()
    // creates a scope tied to the view's lifecycle. scope
    // enables us to launch a coroutine tied to a specific lifecycle
    val coroutineScope = rememberCoroutineScope()

    CoachMeTheme {
        Surface(
            color = MaterialTheme.colors.background,
        ) {
            Scaffold(
                scaffoldState = hamburgerState,
                topBar = {
                    AppBar(
                        title = title,
                        onNavigationIconClick = { coroutineScope.launch { hamburgerState.drawerState.open() } }
                    )
                },
                drawerGesturesEnabled = hamburgerState.drawerState.isOpen,
                drawerContent = {
                    Surface(
                        color = MaterialTheme.colors.background,
                    ) {
                        Column {
                            DrawerHeader(context, UIDisplayed)
                            DrawerBody(
                                items = listOf(
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.PLAN, title = "Map",
                                        contentDescription = "Return to main map",
                                        icon = Default.Map
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.COACHES_LIST,
                                        title = "Nearby coaches",
                                        contentDescription = "See a list of coaches available close to you",
                                        icon = Default.People
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.GROUP_EVENTS_LIST,
                                        title = "Group events",
                                        contentDescription = "See a list of events organized by coaches close to you",
                                        icon = Default.Groups
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.SCHEDULE,
                                        title = "Schedule",
                                        contentDescription = "See schedule",
                                        icon = Default.Today
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.MESSAGING, title = "Chats",
                                        contentDescription = "Go to Messaging section",
                                        icon = Default.Chat
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.PROFILE,
                                        title = "My profile",
                                        contentDescription = "Go to profile",
                                        icon = Default.ManageAccounts
                                    ),
                                    MenuItem(
                                        tag = Dashboard.TestTags.Buttons.LOGOUT, title = "Log out",
                                        contentDescription = "User logs out",
                                        icon = Default.Logout
                                    )
                                ),
                                onItemClick = {
                                    when (it.tag) {
                                        Dashboard.TestTags.Buttons.PLAN -> {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    MapActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.PROFILE -> {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    ProfileActivity::class.java
                                                ),
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.LOGOUT -> {
                                            (context.applicationContext as CoachMeApplication).authenticator.signOut(
                                                context
                                            ) {
                                                (context.applicationContext as CoachMeApplication).store.setCurrentEmail(
                                                    ""
                                                ).thenApply {
                                                    context.startActivity(
                                                        Intent(
                                                            context,
                                                            LoginActivity::class.java
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Dashboard.TestTags.Buttons.SCHEDULE -> {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    ScheduleActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.COACHES_LIST -> {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    CoachesListActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.MESSAGING -> {
                                            val intent =
                                                Intent(context, CoachesListActivity::class.java)
                                            intent.putExtra("isViewingContacts", true)
                                            context.startActivity(intent)
                                        }

                                        Dashboard.TestTags.Buttons.GROUP_EVENTS_LIST -> {
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    GroupEventsListActivity::class.java
                                                )
                                            )
                                        }

                                        else -> {
                                            throw IllegalStateException("Unknown tab clicked: ${it.tag}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                content = { innerPadding ->
                    Surface(
                        color = MaterialTheme.colors.background
                    ) {
                        // invokes dashboardContent composable with correct padding
                        appContent(Modifier.padding(innerPadding))
                    }
                }
            )
        }
    }
}

/**
 * Overloaded version of [Dashboard] that takes a String as title, and displays it as a [Text] composable
 * in the top application bar.
 *
 * @param title = title to display on the top application bar
 * @param appContent = set here the root composable of the current launched activity
 * @param UIDisplayed = future that will be completed when the UI is displayed
 */
@Composable
fun Dashboard(title: String? = null,
              UIDisplayed: CompletableFuture<Void> = CompletableFuture<Void>(),
              appContent: @Composable (Modifier) -> Unit) {
    Dashboard(
        title = { modifier -> Text(text = title ?: stringResource(id = R.string.app_name), modifier = modifier) },
        UIDisplayed = UIDisplayed,
        appContent = appContent
    )
}

@Composable
fun AppBar(title: @Composable (Modifier) -> Unit, onNavigationIconClick: () -> Unit, noElevation: Boolean = false) {
    TopAppBar(
        title = {
            title(Modifier.testTag(BAR_TITLE))
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(HAMBURGER_MENU),
                onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Default.Menu,
                    contentDescription = "Toggle drawer"
                )
            }
        },
        elevation = if (noElevation) 0.dp else AppBarDefaults.TopAppBarElevation
    )
}

@Composable
fun DrawerHeader(context: Context, UIDisplayed: CompletableFuture<Void>) {
    val emailFuture = (context.applicationContext as CoachMeApplication).store.getCurrentEmail()

    var email by remember { mutableStateOf("") }

    LaunchedEffect(emailFuture) {
        email = emailFuture.await()
        UIDisplayed.complete(null)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .testTag(DRAWER_HEADER),
        contentAlignment = Alignment.Center,
        content = {
            Column(horizontalAlignment = CenterHorizontally) {
                Text(text = "Dashboard", fontSize = 40.sp)
            }
        }
    )
}

@Composable
fun DrawerBody(
    items: List<MenuItem>,
    itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(Modifier.testTag(MENU_LIST)) {
        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(16.dp)
                    .testTag(item.tag)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.title,
                    style = itemTextStyle,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

data class MenuItem(
    val tag: String,
    val title: String,
    val contentDescription: String,
    val icon: ImageVector
)


