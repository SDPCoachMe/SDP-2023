package com.github.sdpcoachme.messaging

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.ProfileActivity
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.google.android.gms.tasks.Task
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

        class ChatBox(tag: String) {
            val LABEL = "${tag}Label"
            val BOX = "${tag}Box"
            val COLUMN = "${tag}Column"
        }
        class ChatMessageRow(tag: String) {
            val ROW = "${tag}Row"
            val LABEL = "${tag}Label"
            val TIMESTAMP = "${tag}Timestamp"
        }

        class Buttons {
            companion object {
                const val SEND = "sendButton"
                const val SCROLL_TO_BOTTOM = "scrollToBottomButton"
            }
        }
        companion object {
            val CHAT_FIELD = ChatFieldRow("chatField")
            val CHAT_MESSAGE = ChatMessageRow("chatMessage")
            val CHAT_BOX = ChatBox("chatBox")

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
        .fillMaxHeight()
    ) {

        ContactField(toUser)

        ChatBoxContainer(
            chat = chat,
            currentUserEmail = currentUserEmail,
            database = database,
            chatId = chatId,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag(CHAT_BOX.BOX)
        )


//    val scrollState = rememberScrollState()
//    var onClickScroll by remember { mutableStateOf(false) }
//    val endReached by remember {
//        derivedStateOf {
//            scrollState.value == scrollState.maxValue
//        }
//    }
//
//    LaunchedEffect(chat, onClickScroll) {
//        scrollState.scrollTo(scrollState.maxValue)
//    }
//
//    Column(modifier = Modifier
//        .fillMaxHeight()
//    ) {
//
//        ContactField(toUser)
//
//        // Chat Messages Container
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .weight(1f)
//                .testTag(CHAT_BOX.BOX)
//        ) {
//            // Chat Messages
//            Column(
//                modifier = Modifier
//                    .verticalScroll(scrollState)
//                    .testTag(CHAT_BOX.LABEL)
//            ) {
//
//                ChatMessages(
//                    messages = chat.messages,
//                    currentUserEmail = currentUserEmail,
//                    database = database,
//                    chatId = chatId,
//                    scroll = { onClickScroll = !onClickScroll }
//                )
//            }
//
//            // Scroll to Bottom Button (only visible if not at bottom)
//            if (!endReached) {
//                IconButton(
//                    onClick = { onClickScroll = !onClickScroll },
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .padding(8.dp)
//                        .testTag(SCROLL_TO_BOTTOM)
//                ) {
//                    Icon(
//                        Icons.Filled.ArrowDropDown,
//                        contentDescription = "Scroll to bottom"
//                    )
//                }
//            }
//        }


        ChatField(
            chat = chat,
            currentUserEmail = currentUserEmail,
            database = database,
            chatId = chatId,
            onSend = {
                chatF = database.getChat(chatId)
            })
    }
}




@Composable
fun ChatBoxContainer(chat: Chat, currentUserEmail: String, database: Database, chatId: String, modifier: Modifier) {
    val scrollState = rememberScrollState()
    var onClickScroll by remember { mutableStateOf(false) }
    val endReached by remember {
        derivedStateOf {
            scrollState.value == scrollState.maxValue
        }
    }
    LaunchedEffect(chat, onClickScroll) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
//        Modifier
//            .fillMaxSize()
////            .weight(1f)
//            .testTag(CHAT_BOX.BOX)
    ) {
        // Chat Messages
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .testTag(CHAT_BOX.LABEL)
        ) {

            ChatMessages(
                messages = chat.messages,
                currentUserEmail = currentUserEmail,
                database = database,
                chatId = chatId,
                scroll = { onClickScroll = !onClickScroll }
            )
        }

        // Scroll to Bottom Button (only visible if not at bottom)
        if (!endReached) {
            IconButton(
                onClick = { onClickScroll = !onClickScroll },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag(SCROLL_TO_BOTTOM)
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Scroll to bottom"
                )
            }
        }
    }
}




@Composable
fun ContactField(toUser: UserInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
//            .padding(horizontal = 20.dp, vertical = 20.dp)
            .fillMaxWidth()
            .clickable {
                val coachProfileIntent = Intent(context, ProfileActivity::class.java)
                coachProfileIntent.putExtra("email", toUser.email)
                coachProfileIntent.putExtra("isViewingCoach", true)
                context.startActivity(coachProfileIntent)
            },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = toUser.firstName + " " + toUser.lastName,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.LightGray,
                )
                .padding(start = 30.dp, end = 10.dp, top = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
fun ChatMessages(
    messages: List<Message>,
    currentUserEmail: String,
    database: Database,
    chatId: String,
    scroll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
//            .testTag(CHAT_BOX.BOX)
    ) {
        Column(modifier = Modifier
            .padding(20.dp)
            .testTag(CHAT_BOX.COLUMN)
        ) {
            messages.forEach { message ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag(CHAT_MESSAGE.ROW),
                    horizontalArrangement = if (message.sender == currentUserEmail) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (message.sender == currentUserEmail) Alignment.BottomEnd else Alignment.BottomStart
                    ) {
                        Text(
                            text = message.content.trim() + "\n",
                            modifier = Modifier
                                .testTag(CHAT_MESSAGE.LABEL)
                                .fillMaxWidth(0.7f)
                                .background(
                                    color = if (message.sender == currentUserEmail) Color.Cyan else Color.LightGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                        )

                        Text(
                            text = message.timestamp,
                            modifier = Modifier
                                .testTag(CHAT_MESSAGE.TIMESTAMP)
                                .fillMaxWidth(if (message.sender == currentUserEmail) 0.15f else 0.45f)
                                .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }

//        IconButton(
//            onClick = {
//                scroll()
//            },
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(8.dp)
//                .testTag(SEND)
//        ) {
//            Icon(
//                Icons.Filled.Send,
//                contentDescription = "Send Message"
//            )
//        }
    }
}



@Composable
fun ChatField(chat: Chat, currentUserEmail: String, database: Database, chatId: String, onSend: () -> Unit = {}) {
    val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            .fillMaxWidth()
            .testTag(CHAT_FIELD.ROW),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        // Chat Text Field
        TextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Message") },
            modifier = Modifier
                .weight(0.8f)
                .testTag(CHAT_FIELD.LABEL)
                .background(
                    shape = RoundedCornerShape(30.dp),
                    color = Color.LightGray
                ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
        )

        // Send Button
        if (message.trim().isNotEmpty()) {
            IconButton(
                onClick = {
                    database.sendMessage(
                        chatId,
                        Message(currentUserEmail, message.trim(), LocalDateTime.now().toLocalTime().format(timestampFormatter))
                    ).thenAccept {
                        onSend()
                    }
                    message = ""
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(0.2f)
                    .testTag(SEND)
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send Message"
                )
            }
        }
    }
}

