package com.github.sdpcoachme.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import java.util.concurrent.CompletableFuture

/**
 * Activity responsible for displaying the list of chats
 */
class ChatsOverviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as CoachMeApplication).database

        val currentUserEmail = database.currentUserEmail

        if (currentUserEmail == "") {
            val errorMsg = "The Chats Overview Interface did not receive the current user.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            setContent {
                ChatsOverviewView(database.getUser(currentUserEmail))
            }
        }
    }
}


@Composable
fun ChatsOverviewView(userFuture: CompletableFuture<UserInfo>) {
    var user = UserInfo()

    userFuture.thenAccept { user = it }




}