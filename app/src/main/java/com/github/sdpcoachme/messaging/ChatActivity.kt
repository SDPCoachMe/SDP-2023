package com.github.sdpcoachme.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher

/**
 * Activity responsible for displaying the chat between two users
 */
class ChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get from and to user ids from intent
        val currentUserEmail = intent.getStringExtra("currentUserEmail")
        val toUserEmail = intent.getStringExtra("toUserEmail")

        if (currentUserEmail == null || toUserEmail == null) {
            val errorMsg = "The Chat Interface did not receive both needed users.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val chatId = if (currentUserEmail < toUserEmail) "$currentUserEmail$toUserEmail" else "$toUserEmail$currentUserEmail"

            // get the chat from the database and display it

            setContent {
                ChatView(currentUserEmail, toUserEmail, chatId)
            }
        }
    }
}

@Composable
fun ChatView(currentUserEmail: String, toUserEmail: String, chatId: String) {

}
