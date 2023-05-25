package com.github.sdpcoachme.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.groupevent.GroupEventsListActivity
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.ScheduleActivity
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_NAME
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.dashboardPersonalDetailsBackground
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
            const val DASHBOARD_NAME = "dashboardName"
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
                            Spacer(modifier = Modifier.height(20.dp))
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
                                    fun startActivityWithNoHistory(intent: Intent) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }

                                    when (it.tag) {

                                        Dashboard.TestTags.Buttons.PLAN -> {
                                            startActivityWithNoHistory(
                                                Intent(
                                                    context,
                                                    MapActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.PROFILE -> {
                                            startActivityWithNoHistory(
                                                Intent(
                                                    context,
                                                    ProfileActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.LOGOUT -> {
                                            val authenticator =
                                                (context.applicationContext as CoachMeApplication).authenticator

                                            authenticator.delete(context) {
                                                (context.applicationContext as CoachMeApplication).authenticator.signOut(
                                                    context
                                                ) {
                                                    (context.applicationContext as CoachMeApplication).store.setCurrentEmail(
                                                        ""
                                                    )
                                                        .thenApply {
                                                            startActivityWithNoHistory(
                                                                Intent(
                                                                    context,
                                                                    LoginActivity::class.java
                                                                )
                                                            )
                                                        }
                                                }
                                            }
                                        }

                                        Dashboard.TestTags.Buttons.SCHEDULE -> {
                                            startActivityWithNoHistory(
                                                Intent(
                                                    context,
                                                    ScheduleActivity::class.java
                                                )
                                            )
                                        }

                                        Dashboard.TestTags.Buttons.COACHES_LIST -> {
                                            startActivityWithNoHistory(
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
                                            startActivityWithNoHistory(intent)
                                        }

                                        Dashboard.TestTags.Buttons.GROUP_EVENTS_LIST -> {
                                            startActivityWithNoHistory(
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
        title = { modifier ->
            Text(
                text = title ?: stringResource(id = R.string.app_name),
                modifier = modifier,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
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

/**
 * Composable that displays the header of the drawer.
 * It contains the user's profile picture, name and email.
 *
 * @param context = current context
 * @param UIDisplayed = future that will be completed when the UI is displayed
 */
@Composable
fun DrawerHeader(context: Context, UIDisplayed: CompletableFuture<Void>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.dashboardPersonalDetailsBackground),
        contentAlignment = Alignment.Center,
        content = {
            Column(horizontalAlignment = Alignment.Start) {
                val store = (context.applicationContext as CoachMeApplication).store

                var email by remember { mutableStateOf("") }
                var userInfo by remember { mutableStateOf(UserInfo()) }

                LaunchedEffect(true) {
                    email = store.getCurrentEmail().await()
                    userInfo = store.getUser(email).await()

                    UIDisplayed.complete(null)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 20.dp, 25.dp, 20.dp)
                        .testTag(DRAWER_HEADER),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 3.dp)
                                .testTag(DASHBOARD_NAME),
                            text = userInfo.firstName + " " + userInfo.lastName, fontSize = 20.sp, color = Color.White,
                        )
                        Text(
                            modifier = Modifier
                                .testTag(DASHBOARD_EMAIL)
                                .padding(start = 16.dp),
                            text = email, fontSize = 12.sp, color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Image(
                        painter = painterResource(id = userInfo.getPictureResource()),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Gray, CircleShape)
                            .padding(0.dp, 0.dp, 0.dp, 0.dp)
                            .align(Alignment.CenterEnd)
                            .testTag(ProfileActivity.TestTags.PROFILE_PICTURE)
                    )

                }
                // if in dark mode, add a divider to separate the header from the menu items
                if (!MaterialTheme.colors.isLight) {
                    Divider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                }
            }
        }
    )
}

/**
 * Composable that displays the body of the drawer.
 * It contains the clickable menu items the user can use to navigate through the app.
 *
 * @param items = list of menu items to display
 * @param itemTextStyle = style of the text of the menu items
 * @param onItemClick = callback to invoke when a menu item is clicked
 */
@Composable
fun DrawerBody(
    items: List<MenuItem>,
    itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(Modifier.testTag(MENU_LIST)) {
        items(items) { item ->
            if (item.tag == Dashboard.TestTags.Buttons.LOGOUT) {
                Divider(modifier = Modifier.padding(16.dp))
            }
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


