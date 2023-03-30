package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.FAVORITES
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.HELP
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.SCHEDULE
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.SETTINGS
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.launch

/**
    Dashboard main activity implemented as a left-sided drawer
    to navigate to other application activities. Should be set
    above the main map view.
 */
class DashboardActivity : ComponentActivity() {
    class TestTags {
        companion object {
            const val DRAWER_HEADER = "drawerHeader"
            const val DASHBOARD_EMAIL = "dashboardEmail"
            const val MENU_LIST = "menuList"
        }
        class Buttons {
            companion object {
                const val HAMBURGER_MENU = "hamburgerMenu"
                const val SCHEDULE = "schedule"
                const val PROFILE = "profile"
                const val FAVORITES = "favorites"
                const val SETTINGS = "settings"
                const val HELP = "help"
                const val LOGOUT = "logout"
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO handle the null better here
        val email = intent.getStringExtra("email")

        if (email == null) {
            val errorMsg = "The dashboard did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            setContent {
                CoachMeTheme {
                    DashboardView(email)
                }
            }
        }
    }
}

@Composable
fun DashboardView(email: String) {
    // equivalent to remember { ScaffoldState(...) }
    val scaffoldState = rememberScaffoldState()
    // creates a scope tied to the view's lifecycle. scope
    // enables us to launch a coroutine tied to a specific lifecycle
    val coroutineScope = rememberCoroutineScope()

    Dashboard(
        email = email,
        scaffoldState = scaffoldState,
        onScaffoldStateChange = { coroutineScope.launch { scaffoldState.drawerState.open()} }
    )
}

@Composable
fun Dashboard(email: String, scaffoldState: ScaffoldState, onScaffoldStateChange: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { AppBar(onNavigationIconClick = onScaffoldStateChange) },
        drawerGesturesEnabled = true,
        drawerContent = {
            DrawerHeader(email)
            DrawerBody(
                items = listOf(
                    MenuItem(tag = SCHEDULE, title = "Schedule",
                        contentDescription = "See schedule",
                        icon = Icons.Default.CheckCircle),
                    MenuItem(tag = PROFILE, title = "Profile",
                        contentDescription = "Go to profile",
                        icon = Icons.Default.AccountCircle),
                    MenuItem(tag = FAVORITES, title = "Favorites",
                        contentDescription = "Go to favorites",
                        icon = Icons.Default.Favorite),
                    MenuItem(tag = SETTINGS, title = "Settings",
                        contentDescription = "Go to settings",
                        icon = Icons.Default.Settings),
                    MenuItem(tag = HELP, title = "Help",
                        contentDescription = "Get help",
                        icon = Icons.Default.Info),
                MenuItem(tag = LOGOUT, title = "Log out",
                        contentDescription = "User logs out",
                        icon = Icons.Default.Close)),
                onItemClick = {
                    when (it.tag) {
                        PROFILE -> {
                            val intent = Intent(context, EditProfileActivity::class.java)
                            intent.putExtra("email", email)
                            context.startActivity(intent)
                        }
                        LOGOUT -> {
                            (context.applicationContext as CoachMeApplication).authenticator.signOut(context) {
                                val intent = Intent(context, LoginActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                        SCHEDULE -> {
                            val intent = Intent(context, ScheduleActivity::class.java)
                            intent.putExtra("email", email)
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
        //TODO replace the scaffold content here with the main map view
        content = { innerPadding ->
            // pass the correct padding to the content root, here the column
            Text(modifier = Modifier.padding(innerPadding), text = "Main map view")
        }
    )
}

@Composable
fun AppBar(
    onNavigationIconClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.app_name)) },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(HAMBURGER_MENU),
                onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle drawer"
                )
            }
        }
    )
}

@Composable
fun DrawerHeader(email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .testTag(DRAWER_HEADER),
        contentAlignment = Alignment.Center,
        content = {
            Column(horizontalAlignment = CenterHorizontally) {
                Text(text = "Dashboard", fontSize = 50.sp)
                Text(modifier = Modifier.testTag(DASHBOARD_EMAIL),
                    text = email, fontSize = 20.sp)
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