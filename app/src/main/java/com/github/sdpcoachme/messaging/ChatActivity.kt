package com.github.sdpcoachme.messaging

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.ReadState
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.EventOps
import com.github.sdpcoachme.ui.theme.Purple500
import kotlinx.coroutines.future.await
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
            val READ_STATE = "${tag}READ_STATE"
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

    var stateLoading = CompletableFuture<Void>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = (application as CoachMeApplication).database
        val currentUserEmail = database.getCurrentEmail()

        // TODO: consider refactoring to always pass the chatId
        val chatId = intent.getStringExtra("chatId")
        val isGroupChat: Boolean
        var contact = ""

        if (currentUserEmail == "" || (chatId == null)) {
            val errorMsg = "The Chat Interface did not receive the needed information for the chat.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            isGroupChat = chatId.startsWith("@@event")

            database.getUser(currentUserEmail).thenAccept() { user ->

                contact = if (chatId.startsWith("@@event")) chatId else chatId.replace(currentUserEmail, "")
                // Add the other user to the current user's chat contacts
                if (!user.chatContacts.contains(contact)) {
                    val newUser = user.copy(chatContacts = user.chatContacts + contact)
                    database.updateUser(newUser)
                }
            }.exceptionally { println("error in getUser..."); null }

            // needed to remove the chat listener from the db so that
            // messages are only marked as read when ins the chat
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    database.removeChatListener(chatId)
                    finish()
                }
            })

            setContent {
                ChatView(
                    currentUserEmail,
                    chatId,
                    database,
                    contact,
                    stateLoading,
                    isGroupChat,
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
    toUserEmail: String,
    stateLoading: CompletableFuture<Void>,
    isGroupChat: Boolean = false
) {
    var chat by remember { mutableStateOf(Chat()) }
    var toUser by remember { mutableStateOf(UserInfo()) }

    // TODO: adapt to contain the group event!!!
    var groupEvent by remember { mutableStateOf(GroupEvent()) }

    LaunchedEffect(true) {
        if (isGroupChat) {
            // TODO: get the group event
            groupEvent = database.getGroupEvent(chatId, EventOps.getStartMonday()).await()
        } else {
            toUser = database.getUser(toUserEmail).await()
        }
        chat = database.getChat(chatId).await()
        database.addChatListener(chatId) { newChat ->
            chat = newChat
            database.markMessagesAsRead(chatId, currentUserEmail)
        }

        // Mark all messages addressed to recipient as read
        database.markMessagesAsRead(chatId, currentUserEmail)

        // Activity is now ready for testing
        stateLoading.complete(null)
    }

    Column(modifier = Modifier
        .fillMaxHeight()
    ) {

        ContactField(toUser, chatId, database, isGroupChat, groupEvent)

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
            chat = chat,
            onSend = {
                database.getChat(chatId).thenAccept { chat = it }
            },
            isGroupChat,
            groupEvent,
        )
    }
}

/**
 * Composable responsible for displaying the Contact Field
 */
@Composable
fun ContactField(toUser: UserInfo, chatId: String, database: Database, isGroupChat: Boolean, groupEvent: GroupEvent) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .testTag(CONTACT_FIELD.ROW)
            .fillMaxWidth()
            .height(56.dp) // matches the built-in app bar height
            .background(color = Purple500)
            .clickable { // go to the profile of the other user or show the event
                database.removeChatListener(chatId)

                // if it is a group chat, chatId is the event id
                if (isGroupChat) {
                    // TODO: show the event
                    Toast
                        .makeText(context, "Show the event: $chatId", Toast.LENGTH_LONG)
                        .show()
                } else {
                    // go to the profile of the other user
                    val coachProfileIntent = Intent(context, ProfileActivity::class.java)
                    coachProfileIntent.putExtra("email", toUser.email)
                    // TODO:
                    //  once users can also message other users and not just coaches,
                    //  this should be adapted to also work for users
                    coachProfileIntent.putExtra("isViewingCoach", true)
                    context.startActivity(coachProfileIntent)
                }
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
            text = if (isGroupChat) groupEvent.event.name else toUser.firstName + " " + toUser.lastName,
            fontSize = 20.sp,
            modifier = Modifier
                .testTag(CONTACT_FIELD.LABEL)
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(start = 10.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
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
    val scrollState = rememberScrollState(Int.MAX_VALUE)
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
                currentUserEmail = currentUserEmail,
                nbParticipants = chat.participants.size
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
    currentUserEmail: String,
    nbParticipants: Int
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
                    isFromCurrentUser = isFromCurrentUser,
                    nbParticipants = nbParticipants
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
               isFromCurrentUser: Boolean,
               nbParticipants: Int
) {
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
            // message content (if the sender is not clear from the context
            // (i.e., if it is a group chat with > 2 participants), display the sender's name)
            val msgContent = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold)
                ) { if (!isFromCurrentUser && nbParticipants > 2) append(message.senderName + "\n") }
                append(message.content.trim() + "\n")
            }
            Text(
                text = msgContent,
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
            // once online/offline mode implemented, we could add the single + double check mark functionality
            // similar to whatsapp
            if (message.sender == currentUserEmail) {
                val imgVector: ImageVector
                val contentDescr: String
                val color: Color
                when (message.readState) {
                    ReadState.SENT -> {
                        imgVector = Icons.Default.Check
                        contentDescr = "message sent icon"
                        color = timeAndUnreadMarkColor
                    }
                    ReadState.RECEIVED -> {
                        imgVector = Icons.Default.DoneAll
                        contentDescr = "message received icon"
                        color = timeAndUnreadMarkColor
                    }
                    ReadState.READ -> {
                        imgVector = Icons.Default.DoneAll
                        contentDescr = "message read icon"
                        color = readMarkColor
                    }
                }

                Icon(
                    imageVector = imgVector,
                    contentDescription = contentDescr,
                    tint = color,
                    modifier = Modifier
                        .testTag(CHAT_MESSAGE.READ_STATE)
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
fun ChatField(
    currentUserEmail: String,
    database: Database,
    chat: Chat,
    onSend: () -> Unit = {},
    isGroupChat: Boolean,
    groupEvent: GroupEvent,
) {
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
                    database.getUser(currentUserEmail).thenCompose {
                        database.sendMessage(
                            chat.id,
                            Message(
                                currentUserEmail,
                                "${it.firstName} ${it.lastName}",
                                message.trim(),
                                LocalDateTime.now().toString(),
                                ReadState.SENT
                            )
                        ).thenAccept {
                            onSend()
                        }
                    }
                    message = ""

                    // Place this chat at the top of the users' chat list whenever a message is sent
                    for (participantMail in chat.participants) {
                        database.getUser(participantMail).thenCompose {
                            val contact = if (isGroupChat) groupEvent.groupEventId else chat.id.replace(participantMail, "")
                            println("contact to update: $contact")
                            println("user whos contact list is being updated: ${it.email}")
                            database.updateUser(it.copy(chatContacts = listOf(contact) + it.chatContacts.filter { e -> e != contact }))
                        }
                    }
                    // TODO: remove this after testing
//                    // place this chat at the top of the users chat list whenever they send a message
//                    database.getUser(database.getCurrentEmail()).thenCompose {
//                        database.updateUser(it.copy(chatContacts = listOf(toUser.email) + it.chatContacts.filter { e -> e != toUser.email }))
//                    }
//                    //same for the toUser
//                    database.getUser(toUser.email).thenCompose {
//                        database.updateUser(it.copy(chatContacts = listOf(database.getCurrentEmail()) + it.chatContacts.filter { e -> e != database.getCurrentEmail() }))
//                    }
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

