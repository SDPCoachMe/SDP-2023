package com.github.sdpcoachme

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import kotlinx.coroutines.launch

/**
    Dashboard main activity implemented as a left-sided drawer
    to navigate to other application activities. Should be set
    above the main map view.
 */
class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CoachMeTheme {
                DashboardView()
            }
        }
    }
}

@Composable
fun DashboardView() {
    // equivalent to remember { ScaffoldState(...) }
    val scaffoldState = rememberScaffoldState()
    // creates a scope tied to the view's lifecycle. scope
    // enables us to launch a coroutine tied to a specific lifecycle
    val coroutineScope = rememberCoroutineScope()

    Dashboard(
        scaffoldState = scaffoldState,
        onScaffoldStateChange = { coroutineScope.launch { scaffoldState.drawerState.open()} }
    )
}

@Composable
fun Dashboard(scaffoldState: ScaffoldState, onScaffoldStateChange: () -> Unit) {

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { AppBar(onNavigationIconClick = onScaffoldStateChange) },
        drawerGesturesEnabled = true,
        drawerContent = {
            DrawerHeader()
            DrawerBody(
                modifier = Modifier.testTag("drawerBody"),
                items = listOf(
                    MenuItem(id = "schedule", title = "Schedule",
                        contentDescription = "See schedule",
                        icon = Icons.Default.CheckCircle),
                    MenuItem(id = "profile", title = "Profile",
                        contentDescription = "Go to profile",
                        icon = Icons.Default.AccountCircle),
                    MenuItem(id = "favorite", title = "Favorites",
                        contentDescription = "Go to favorites",
                        icon = Icons.Default.Favorite),
                    MenuItem(id = "settings", title = "Settings",
                        contentDescription = "Go to settings",
                        icon = Icons.Default.Settings),
                    MenuItem(id = "help", title = "Help",
                        contentDescription = "Get help",
                        icon = Icons.Default.Info)),
                onItemClick = {
                    // TODO call the associated fragment/activity associated with it
                    println("Clicked on ${it.title}")})},
        content = { innerPadding ->
            // pass the correct padding to the content root, here the column
            LazyColumn(contentPadding = innerPadding) {
                items(count = 100) {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp))
                }
            }
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
                modifier = Modifier.testTag("appBarMenuIcon"),
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
fun DrawerHeader() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
        content = { Text(text = "Dashboard", fontSize = 50.sp) }
    )
}

@Composable
fun DrawerBody(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(modifier) {
        items(items) { item ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(16.dp)
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
    val id: String,
    val title: String,
    val contentDescription: String,
    val icon: ImageVector
)