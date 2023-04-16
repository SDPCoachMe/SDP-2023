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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.data.ListItem
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.map.MapActivity
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.util.concurrent.CompletableFuture

class SelectSportsActivity : ComponentActivity() {

    class TestTags {
        class ListRowTag(tag: String) {
            val TEXT = "${tag}Text"
            val ICON = "${tag}Icon"
            val ROW = "${tag}Row"
        }
        class MultiSelectListTag {
            companion object {
                const val LAZY_SELECT_COLUMN = "lazySelectColumn"
                val ROW_TEXT_LIST = Sports.values().map { ListRowTag(it.sportName) }
            }
        }
        class Buttons {
            companion object {
                const val REGISTER = "registerButton"
            }
        }
        companion object {
            const val LIST_TITLE = "listTitle"
            const val COLUMN = "column"
        }
    }

    private lateinit var database : Database
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()

        if(email.isEmpty()) {
            val errorMsg = "The sport selection did not receive an email address." +
                    "\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        }

        val isEditingProfile = intent.getBooleanExtra("isEditingProfile", false)
        setContent {
            CoachMeTheme {
                FavoriteSportsSelection(isEditingProfile, database.getUser(email))
            }
        }
    }

    @Composable
    fun FavoriteSportsSelection(isEditingProfile: Boolean, userFuture: CompletableFuture<UserInfo>) {
        val context = LocalContext.current
        var sportItems by remember {
            mutableStateOf(Sports.values().map { ListItem(it, false) })
        }
        var userInfo by remember { mutableStateOf(userFuture) }

        val toggleSelectSport: (Sports) -> Unit = { sport ->
            sportItems = sportItems.map { item ->
                if (item.element == sport) {
                    item.copy(selected = !item.selected)
                } else {
                    item
                }
            }
        }

        userInfo.thenAccept { user ->
            if (user != null) {
                sportItems = sportItems.map { item ->
                    if (user.sports.contains(item.element)) {
                        item.copy(selected = true)
                    } else {
                        item
                    }
                }
                userInfo = CompletableFuture.completedFuture(null)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag(TestTags.COLUMN),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select your favorite sports:",
                modifier = Modifier.testTag(TestTags.LIST_TITLE)
            )
            MultiSelectList(
                items = sportItems,
                toggleSelectSport = toggleSelectSport
            )
            Button(
                modifier = Modifier.testTag(TestTags.Buttons.REGISTER),
                onClick = {
                    database.getUser(email)
                        .thenApply { user -> user.copy(
                            sports = sportItems.filter { it.selected }.map { it.element })
                        }
                        .thenApply { user -> database.updateUser(user) }
                        .thenApply {
                            val targetClass =
                                if (isEditingProfile) ProfileActivity::class.java
                                else MapActivity::class.java
                            val intent = Intent(context, targetClass)
                            startActivity(intent)
                        }.exceptionally {
                            println("inside the error handler $it")
                            val errorMsg = "There was a database error.\nPlease return to the login page and try again."
                            ErrorHandlerLauncher().launchExtrasErrorHandler(
                                context, errorMsg)
                        }
                }
            )
            { Text("REGISTER") }

        }
    }

    @Composable
    fun MultiSelectList(items: List<ListItem<Sports>>, toggleSelectSport: (Sports) -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.MultiSelectListTag.LAZY_SELECT_COLUMN)
        ) {
            items(items.size) { i ->
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            toggleSelectSport(Sports.values()[i])
                        }
                        .padding(16.dp)
                        .testTag(TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].ROW),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = items[i].element.sportName,
                        modifier = Modifier.testTag(
                            TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].TEXT))
                    if (items[i].selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag(TestTags.MultiSelectListTag.ROW_TEXT_LIST[i].ICON)
                        )
                    }
                }
            }
        }
    }
}