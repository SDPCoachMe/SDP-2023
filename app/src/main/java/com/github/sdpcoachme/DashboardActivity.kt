package com.github.sdpcoachme

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.core.content.ContextCompat
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
import com.github.sdpcoachme.data.MapState
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.map.MapViewModel
import com.github.sdpcoachme.ui.MapView
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
            const val MAP = "map"
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

    /**
     * Create an activity for result : display window to request asked permission.
     * If granted, launches the callback (here getDeviceLocation(...) which retrieves the user's
     * location). The contract is a predefined "function" which takes a permission as input and
     * outputs if the user has granted it or not.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean -> if (isGranted) {
                    mapViewModel.getDeviceLocation(fusedLocationProviderClient)
                } else {
                    // TODO Permission denied or only COARSE given
                }
        }

    /**
     * This function updates the location state of the MapViewModel if the permission is granted.
     * If the permission is denied, it requests it.
     */
    private fun getLocation() =
        when (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                mapViewModel.getDeviceLocation(fusedLocationProviderClient)
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val mapViewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO handle the null better here
        val email = intent.getStringExtra("email")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        if (email == null) {
            val errorMsg = "The dashboard did not receive an email address.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            setContent {
                CoachMeTheme {
                    DashboardView(email, mapViewModel.mapState.value)
                }
            }
        }
    }
}

@Composable
fun DashboardView(email: String, mapState: MapState) {
    // equivalent to remember { ScaffoldState(...) }
    val scaffoldState = rememberScaffoldState()
    // creates a scope tied to the view's lifecycle. scope
    // enables us to launch a coroutine tied to a specific lifecycle
    val coroutineScope = rememberCoroutineScope()

    Dashboard(
        email = email,
        scaffoldState = scaffoldState,
        onScaffoldStateChange = { coroutineScope.launch { scaffoldState.drawerState.open()} },
        mapState = mapState
    )
}

@Composable
fun Dashboard(email: String,
              scaffoldState: ScaffoldState,
              onScaffoldStateChange: () -> Unit,
              mapState: MapState
) {
    val context = LocalContext.current

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { AppBar(onNavigationIconClick = onScaffoldStateChange) },
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
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
            MapView(
                modifier = Modifier.padding(innerPadding),
                mapState = mapState
            )
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