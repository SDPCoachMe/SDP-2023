package com.github.sdpcoachme.messaging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
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

        class ChatMessageRow(tag: String) {
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
            val CHAT_MESSAGE = ChatMessageRow("chatMessage")

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
                ChatView(currentUserEmail, toUserEmail, chatId, database, database.getChat(chatId), database.getUser(currentUserEmail), database.getUser(toUserEmail))
            }
        }
    }
}

@Composable
fun ChatView(currentUserEmail: String,
             toUserEmail: String,
             chatId: String,
             database: Database,
             chatFuture: CompletableFuture<Chat>,
             fromUserFuture: CompletableFuture<UserInfo>,
             toUserFuture: CompletableFuture<UserInfo>
) {
    var chat by remember { mutableStateOf(Chat()) }
    var fromUser by remember { mutableStateOf(UserInfo()) }
    var toUser by remember { mutableStateOf(UserInfo()) }



    var chatF by remember { mutableStateOf(chatFuture) }
    var fromUserF by remember { mutableStateOf(fromUserFuture) }
    var toUserF by remember { mutableStateOf(toUserFuture) }


    chatF.thenAccept {
        if (it != null) {
            chat = it
            chatF = CompletableFuture.completedFuture(null)
        }
    }

    fromUserF.thenAccept {
        if (it != null) {
            fromUser = it
            fromUserF = CompletableFuture.completedFuture(null)
        }
    }

    toUserF.thenAccept {
        if (it != null) {
            toUser = it
            toUserF = CompletableFuture.completedFuture(null)
        }
    }

    Column(modifier = Modifier
        .fillMaxHeight()) {

        // Chat Messages

        ContactField(toUser)
        ChatMessages(chat.messages, currentUserEmail, database, chatId)

        Spacer(modifier = Modifier.weight(1f))

        ChatField(chat, currentUserEmail, database, chatId)
    }
}

@Composable
fun ChatMessages(
    messages: List<Message>,
    currentUserEmail: String,
    database: Database,
    chatId: String
) {
    Column(modifier = Modifier.padding(20.dp)) {
        messages.forEach { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag(CHAT_MESSAGE.ROW),
                horizontalArrangement = if (message.sender == currentUserEmail) Arrangement.End else Arrangement.Start,
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier
                        .testTag(CHAT_MESSAGE.LABEL)
                        .padding(10.dp)
                        .weight(0.8f, false)
                        .fillMaxWidth(0.7f),
//                        .wrapContentWidth(if (message.sender == currentUserEmail) Alignment.End else Alignment.Start)
                )
            }
        }
    }
}


@Composable
fun ChatField(chat: Chat, currentUserEmail: String, database: Database, chatId: String, onSend: () -> Unit = {}) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .fillMaxWidth()
            .testTag(CHAT_FIELD.ROW),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        // Text Field
        TextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Message") },
            modifier = Modifier.weight(0.8f)
                .testTag(CHAT_FIELD.LABEL)
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

@Composable
fun ContactField(toUser: UserInfo) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(toUser.firstName + " " + toUser.lastName)
    }
}

