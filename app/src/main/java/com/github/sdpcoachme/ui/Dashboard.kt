package com.github.sdpcoachme.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
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
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.COACHES_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.GROUP_EVENTS_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.MESSAGING
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PLAN
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.SCHEDULE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
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
 * @param title = title to display on the top application bar
 */
@Composable
fun Dashboard(title: String? = null,
              UIDisplayed: CompletableFuture<Void> = CompletableFuture<Void>(),
              appContent: @Composable (Modifier) -> Unit) {

    val context = LocalContext.current
    // equivalent to remember { ScaffoldState(...) }
    val scaffoldState = rememberScaffoldState()
    // creates a scope tied to the view's lifecycle. scope
    // enables us to launch a coroutine tied to a specific lifecycle
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AppBar(
                title = title ?: stringResource(id = R.string.app_name),
                onNavigationIconClick = { coroutineScope.launch {scaffoldState.drawerState.open()} }
            )},
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        drawerContent = {
            DrawerHeader(context, UIDisplayed)
            DrawerBody(
                items = listOf(
                    MenuItem(tag = PLAN, title = "Map",
                        contentDescription = "Return to main map",
                        icon = Default.Map),
                    MenuItem(tag = COACHES_LIST, title = "Nearby coaches",
                        contentDescription = "See a list of coaches available close to you",
                        icon = Default.People),
                    MenuItem(tag = GROUP_EVENTS_LIST, title = "Group events",
                        contentDescription = "See a list of events organized by coaches close to you",
                        icon = Default.Groups),
                    MenuItem(tag = SCHEDULE, title = "Schedule",
                        contentDescription = "See schedule",
                        icon = Default.CheckCircle),
                    MenuItem(tag = MESSAGING, title = "Messaging",
                        contentDescription = "Go to Messaging section",
                        icon = Default.Chat),
                    MenuItem(tag = PROFILE, title = "My profile",
                        contentDescription = "Go to profile",
                        icon = Default.AccountCircle),
                    MenuItem(tag = LOGOUT, title = "Log out",
                            contentDescription = "User logs out",
                            icon = Default.Logout)
                ),
                onItemClick = {
                    when (it.tag) {
                        PLAN -> {
                            context.startActivity(Intent(context, MapActivity::class.java))
                        }
                        PROFILE -> {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                        }
                        LOGOUT -> {
                            (context.applicationContext as CoachMeApplication).authenticator.signOut(context) {
                                (context.applicationContext as CoachMeApplication).store.setCurrentEmail("")
                                    .thenApply {
                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                    }
                            }
                        }
                        SCHEDULE -> {
                            context.startActivity(Intent(context, ScheduleActivity::class.java))
                        }
                        COACHES_LIST -> {
                            context.startActivity(Intent(context, CoachesListActivity::class.java))
                        }
                        MESSAGING -> {
                            val intent = Intent(context, CoachesListActivity::class.java)
                            intent.putExtra("isViewingContacts", true)
                            context.startActivity(intent)
                        }
                        GROUP_EVENTS_LIST -> {
                            context.startActivity(Intent(context, GroupEventsListActivity::class.java))
                        }
                        else -> {
                            throw IllegalStateException("Unknown tab clicked: ${it.tag}")
                        }
                    }
                }
            )
        },
        content = { innerPadding ->
            // invokes dashboardContent composable with correct padding
            appContent(Modifier.padding(innerPadding))
        }
    )
}

@Composable
fun AppBar(title: String, onNavigationIconClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = title, modifier = Modifier.testTag(BAR_TITLE)) },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(HAMBURGER_MENU),
                onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Default.Menu,
                    contentDescription = "Toggle drawer"
                )
            }
        }
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
                Text(text = "Dashboard", fontSize = 50.sp)
                Text(
                    modifier = Modifier.testTag(DASHBOARD_EMAIL),
                    text = email, fontSize = 20.sp
                )
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


