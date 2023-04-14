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
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachesListActivity
import com.github.sdpcoachme.ProfileActivity
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.ui.theme.Purple500
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

/**
 * Activity responsible for displaying the chat between two users
 */
class ChatActivity : ComponentActivity() {

    class TestTags {

        class ContactFieldRow(tag: String) {
            val LABEL = "${tag}Label"
            val ROW = "${tag}Row"
        }

        class ChatFieldRow(tag: String) {
            val LABEL = "${tag}Label"
            val ROW = "${tag}Row"
        }

        class ChatBox(tag: String) {
            val CONTAINER = "${tag}Container"
            val SCROLL_COLUMN = "${tag}ScrollColumn"
            val MESSAGES_BOX = "${tag}Box"
            val MESSAGES_COLUMN = "${tag}Column"
        }
        class ChatMessageRow(tag: String) {
            val ROW = "${tag}Row"
            val LABEL = "${tag}Label"
            val TIMESTAMP = "${tag}Timestamp"
            val DATE_ROW = "${tag}DateRow"
            val IS_READ = "${tag}IsRead"
        }

        class Buttons {
            companion object {
                const val SEND = "sendButton"
                const val SCROLL_TO_BOTTOM = "scrollToBottomButton"
                const val BACK = "backButton"
            }
        }
        companion object {
            val CONTACT_FIELD = ContactFieldRow("contactField")
            val CHAT_FIELD = ChatFieldRow("chatField")
            val CHAT_MESSAGE = ChatMessageRow("chatMessage")
            val CHAT_BOX = ChatBox("chatBox")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = (application as CoachMeApplication).database
        val currentUserEmail = database.getCurrentEmail()
        val toUserEmail = intent.getStringExtra("toUserEmail")

        if (currentUserEmail == "" || toUserEmail == null) {
            val errorMsg = "The Chat Interface did not receive both needed users.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val chatId = if (currentUserEmail < toUserEmail) "$currentUserEmail$toUserEmail" else "$toUserEmail$currentUserEmail"

            database.getUser(currentUserEmail).thenAccept() { user ->
                // Add the other user to the current user's chat contacts
                if (!user.chatContacts.contains(toUserEmail)) {
                    val newUser = user.copy(chatContacts = user.chatContacts + toUserEmail)
                    database.updateUser(newUser)
                }
            }

            // Mark all messages addressed to recipient as read
            database.markMessagesAsRead(chatId, currentUserEmail)

            setContent {
                ChatView(
                    currentUserEmail,
                    chatId,
                    database,
                    database.getChat(chatId),
                    database.getUser(currentUserEmail),
                    database.getUser(toUserEmail)
                )
            }
        }
    }
}

/**
 * Composable responsible for displaying the chat between two users
 */
@Composable
fun ChatView(
    currentUserEmail: String,
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

            database.removeChatListener(chatId) // done to remove the old one
            database.addChatListener(chatId) {
                newChat -> chat = newChat
                database.markMessagesAsRead(chatId, currentUserEmail)
            }
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

        ContactField(toUser, chatId, database)

        ChatBoxContainer(
            chat = chat,
            currentUserEmail = currentUserEmail,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag(CHAT_BOX.CONTAINER)
        )

        ChatField(
            currentUserEmail = currentUserEmail,
            database = database,
            chatId = chatId,
            onSend = {
                chatF = database.getChat(chatId)
            },
            toUser = toUser
        )
    }
}

/**
 * Composable responsible for displaying the Contact Field
 */
@Composable
fun ContactField(toUser: UserInfo, chatId: String, database: Database) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .testTag(CONTACT_FIELD.ROW)
            .fillMaxWidth()
            .background(color = Purple500)
            .clickable {
                val coachProfileIntent = Intent(context, ProfileActivity::class.java)
                coachProfileIntent.putExtra("email", toUser.email)
                // once users can also message other users and not just coaches,
                // this should be adapted to also work for users
                coachProfileIntent.putExtra("isViewingCoach", true)
                context.startActivity(coachProfileIntent)
            },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        // Button icon for the back button
        IconButton(
            onClick = {
                database.removeChatListener(chatId)
                // go back to the listed contacts (msg contacts or coaches)
                val intent = Intent(context, CoachesListActivity::class.java)
                intent.putExtra("isViewingContacts", true)
                context.startActivity(intent)

            },
            modifier = Modifier
                .testTag(BACK)
                .padding(start = 5.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = toUser.firstName + " " + toUser.lastName,
            fontSize = 20.sp,
            modifier = Modifier
                .testTag(CONTACT_FIELD.LABEL)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp, top = 20.dp, bottom = 20.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Container for the chat box
 *
 * enables scrolling within the chat box and the scroll-to-bottom button
 */
@Composable
fun ChatBoxContainer(
    chat: Chat,
    currentUserEmail: String,
    modifier: Modifier
) {
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
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .testTag(CHAT_BOX.SCROLL_COLUMN)
        ) {
            // Chat Messages
            ChatMessages(
                messages = chat.messages,
                currentUserEmail = currentUserEmail
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

/**
 * Displays the messages in the chat
 */
@Composable
fun ChatMessages(
    messages: List<Message>,
    currentUserEmail: String
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Box(
        modifier = Modifier
            .testTag(CHAT_BOX.MESSAGES_BOX)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier
            .padding(20.dp)
            .testTag(CHAT_BOX.MESSAGES_COLUMN)
        ) {
            // Chat Messages
            var lastDate: LocalDateTime = LocalDateTime.MIN
            messages.forEach { message ->
                val isFromCurrentUser = message.sender == currentUserEmail
                val currDateTime = LocalDateTime.parse(message.timestamp)

                // check if new date should be displayed in chat
                if (currDateTime.toLocalDate() != lastDate.toLocalDate()) {
                    lastDate = currDateTime
                    Text(
                        text = lastDate.format(dateFormatter),
                        modifier = Modifier
                            .testTag(CHAT_MESSAGE.DATE_ROW)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 10.dp)
                            .background(
                                color = Color(0xFFE6E7E7),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(5.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                MessageRow(
                    message = message,
                    currentUserEmail = currentUserEmail,
                    isFromCurrentUser = isFromCurrentUser
                )
            }
        }
    }
}

/**
 * Displays a single message in the chat
 */
@Composable
fun MessageRow(message: Message,
               currentUserEmail: String,
               isFromCurrentUser: Boolean) {
    val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeAndUnreadMarkColor = Color(0xFF6C6C6D)
    val readMarkColor = Color(0xFF0027FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .testTag(CHAT_MESSAGE.ROW),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isFromCurrentUser) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            // message content
            Text(
                text = message.content.trim() + "\n",
                modifier = Modifier
                    .testTag(CHAT_MESSAGE.LABEL)
                    .fillMaxWidth(0.7f)
                    .background(
                        color = if (isFromCurrentUser) Color(0xFFBBC5FD) else Color.LightGray,
                        shape = RoundedCornerShape(
                            10.dp,
                            10.dp,
                            if (isFromCurrentUser) 0.dp else 10.dp,
                            if (isFromCurrentUser) 10.dp else 0.dp
                        )
                    )
                    .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),

                )

            // timestamp for message
            Text(
                text = LocalDateTime.parse(message.timestamp).toLocalTime().format(timestampFormatter),
                color = timeAndUnreadMarkColor,
                modifier = Modifier
                    .testTag(CHAT_MESSAGE.TIMESTAMP)
                    .fillMaxWidth(if (message.sender == currentUserEmail) 0.19f else 0.45f)
                    .padding(start = 5.dp, end = 0.dp, top = 5.dp, bottom = 2.dp)
                    .align(Alignment.BottomEnd)
            )

            // read by recipient icon
            // once online/offline mode implemented, we could add the single + double check mark funcitonality
            // similar to whatsapp
            if (message.sender == currentUserEmail) {
                Icon(
                    imageVector = if (message.readByRecipient) Icons.Default.DoneAll else Icons.Default.Check,
                    contentDescription = (if (message.readByRecipient) "" else "Not ") + "read by recipient icon",
                    tint = if (message.readByRecipient) readMarkColor else timeAndUnreadMarkColor,
                    modifier = Modifier
                        .testTag(CHAT_MESSAGE.IS_READ)
                        .fillMaxWidth(0.07f)
                        .padding(start = 0.dp, end = 0.dp, top = 5.dp, bottom = 2.dp)
                        .align(Alignment.BottomEnd)
                        .height(16.dp),
                )
            }
        }
    }
}

/**
 * Chat field where user can type and send messages
 */
@Composable
fun ChatField(currentUserEmail: String,
              database: Database,
              chatId: String,
              onSend: () -> Unit = {},
              toUser: UserInfo) {
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
                        Message(currentUserEmail, message.trim(), LocalDateTime.now().toString(), false)
                    ).thenAccept {
                        onSend()
                    }
                    message = ""
                    // place this chat at the top of the users chat list whenever they send a message
                    database.getUser(database.getCurrentEmail()).thenCompose {
                        database.updateUser(it.copy(chatContacts = listOf(toUser.email) + it.chatContacts.filter { e -> e != toUser.email }))
                    }
                    //same for the toUser
                    database.getUser(toUser.email).thenCompose {
                        database.updateUser(it.copy(chatContacts = listOf(database.getCurrentEmail()) + it.chatContacts.filter { e -> e != database.getCurrentEmail() }))
                    }


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

