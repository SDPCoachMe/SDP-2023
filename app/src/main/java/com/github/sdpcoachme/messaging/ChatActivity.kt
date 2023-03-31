package com.github.sdpcoachme.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import java.util.concurrent.CompletableFuture

/**
 * Activity responsible for displaying the chat between two users
 */
class ChatActivity : ComponentActivity() {

    class TestTags {
        class ChatFieldRow(tag: String) {
            val LABEL = "${tag}Label"
            val ROW = "${tag}Row"
        }
        class Buttons {
            companion object {
                const val SEND = "sendButton"
            }
        }
        companion object {
            val CHAT_FIELD = ChatFieldRow("chatField")

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get from and to user ids from intent
        val currentUserEmail = intent.getStringExtra("currentUserEmail")
        val toUserEmail = intent.getStringExtra("toUserEmail")
        val database = (application as CoachMeApplication).database

        if (currentUserEmail == null || toUserEmail == null) {
            val errorMsg = "The Chat Interface did not receive both needed users.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val chatId = if (currentUserEmail < toUserEmail) "$currentUserEmail$toUserEmail" else "$toUserEmail$currentUserEmail"

            setContent {
                ChatView(currentUserEmail, toUserEmail, chatId, database, database.getChat(chatId))
            }
        }
    }
}

@Composable
fun ChatView(currentUserEmail: String, toUserEmail: String, chatId: String, database: Database, chatFuture: CompletableFuture<Chat>) {
    var chat by remember { mutableStateOf(Chat()) }
    var chatF by remember { mutableStateOf(chatFuture) }

    chatF.thenAccept {
        if (it != null) {
            chat = it
            chatF = CompletableFuture.completedFuture(null)
        }
    }
}


@Composable
fun ChatField(chat: Chat, currentUserEmail: String, database: Database, chatId: String) {
    var message by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 80.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Text Field
        TextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Message") },
            modifier = Modifier.weight(0.8f)
        )

        // Send Button
        if (message.isNotEmpty()) {

            IconButton(
                onClick = {
                    database.sendMessage(chatId, Message(currentUserEmail, message))
                    message = ""
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(0.2f)
                    .testTag(SEND)
                // place it on the right side of the screen

            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send Message"
                )
            }
        }
    }
}

