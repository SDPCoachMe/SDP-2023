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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.ScheduleActivity
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.COACHES_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HELP
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.MESSAGING
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PLAN
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.SCHEDULE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.SETTINGS
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
import kotlinx.coroutines.launch

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
                const val COACHES_LIST = "coacheslist"
                const val MESSAGING = "Messaging"
                const val SETTINGS = "settings"
                const val HELP = "help"
                const val LOGOUT = "logout"
            }
        }
    }
}

/**
 * Dashboard UI implemented as a left-sided drawer to navigate to other application activities.
 * @param appContent = set here the root composable of the current launched activity
 */
@Composable
fun Dashboard(appContent: @Composable (Modifier) -> Unit) {
    Dashboard(null, appContent)
}

/**
 * Dashboard UI implemented as a left-sided drawer to navigate to other application activities.
 * @param appContent = set here the root composable of the current launched activity
 * @param title = title to display on the top application bar
 */
@Composable
fun Dashboard(title: String?, appContent: @Composable (Modifier) -> Unit) {

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
            DrawerHeader(context)
            DrawerBody(
                items = listOf(
                    MenuItem(tag = PLAN, title = "Map",
                        contentDescription = "Return to main map",
                        icon = Default.LocationOn),
                    MenuItem(tag = SCHEDULE, title = "Schedule",
                        contentDescription = "See schedule",
                        icon = Default.CheckCircle),
                    MenuItem(tag = PROFILE, title = "My profile",
                        contentDescription = "Go to profile",
                        icon = Default.AccountCircle),
                    MenuItem(tag = COACHES_LIST, title = "Nearby coaches",
                        contentDescription = "See a list of coaches available close to you",
                        icon = Default.People),
                    MenuItem(tag = MESSAGING, title = "Messaging",
                        contentDescription = "Go to Messaging section",
                        icon = Default.Message),
                    MenuItem(tag = SETTINGS, title = "Settings",
                        contentDescription = "Go to settings",
                        icon = Default.Settings),
                    MenuItem(tag = HELP, title = "Help",
                        contentDescription = "Get help",
                        icon = Default.Info),
                    MenuItem(tag = LOGOUT, title = "Log out",
                            contentDescription = "User logs out",
                            icon = Default.Close)
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
                                context.startActivity(Intent(context, LoginActivity::class.java))
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
                        else -> {
                            // TODO replace the print by a call to the corresponding item activity
                            println("Clicked on ${it.title}")

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
fun DrawerHeader(context: Context) {
    val email = (context.applicationContext as CoachMeApplication).store.getCurrentEmail()

    if (email.isEmpty()) {
        val errorMsg = "Dashboard did not receive an email address.\n Please return to the login page and try again."
        ErrorHandlerLauncher().launchExtrasErrorHandler(context, errorMsg)
    } else {
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


