package com.github.sdpcoachme.messaging

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
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
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.ReadState
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.ui.theme.*
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
    private lateinit var store: CachingStore
    private lateinit var emailFuture: CompletableFuture<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = (application as CoachMeApplication).store
        emailFuture = store.getCurrentEmail()
            .exceptionally {
                // The following recovers from the user receiving a push notification, then logging out
                // and then clicking on the notification. In this case, the intent will contain the email
                val pushNotificationEmail = intent.getStringExtra("pushNotification_currentUserEmail")!!
                store.setCurrentEmail(pushNotificationEmail)
                pushNotificationEmail
            }
        val chatId = intent.getStringExtra("chatId")
        val isGroupChat: Boolean
        var contact = ""

        if (chatId == null) {
            val errorMsg =
                "The Chat Interface did not receive the needed information for the chat.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
            stateLoading.complete(null)
        } else {
            isGroupChat = chatId.startsWith("@@event")

            emailFuture.thenCompose { email ->
                contact =
                    if (isGroupChat) chatId
                    else chatId.replace(email, "")

                store.addChatContactIfNew(email, chatId, contact)
            }

            // needed to remove the chat listener from the db so that
            // messages are only marked as read when in the chat
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.removeChatListener(chatId)
                    finish()
                }
            })

            setContent {
                CoachMeTheme {
                    Surface(color = MaterialTheme.colors.background) {
                        ChatView(chatId, contact, stateLoading, isGroupChat)
                    }
                }
            }
        }
    }


    /**
     * Composable responsible for displaying the chat between two users
     */
    @Composable
    fun ChatView(
        chatId: String,
        toUserEmail: String,
        stateLoading: CompletableFuture<Void>,
        isGroupChat: Boolean
    ) {
        var chat by remember { mutableStateOf(Chat()) }
        var toUser by remember { mutableStateOf(UserInfo()) }
        var currentUserEmail by remember { mutableStateOf("") }
        var groupEvent by remember { mutableStateOf(GroupEvent()) }

        LaunchedEffect(true) {
            if (isGroupChat) {
                groupEvent = store.getGroupEvent(chatId).await()
            } else {
                toUser = store.getUser(toUserEmail).await()
            }
            currentUserEmail = emailFuture.await()
            chat = store.getChat(chatId).await()
            store.addChatListener(chatId) { newChat ->
                chat = newChat
                store.markMessagesAsRead(chatId, currentUserEmail)
            }

            // Mark all messages addressed to recipient as read
            store.markMessagesAsRead(chatId, currentUserEmail)

            // Activity is now ready for testing
            stateLoading.complete(null)
        }

        Scaffold(
            topBar = {
                ContactField(toUser, chatId, isGroupChat, groupEvent)
            }
        ) { padding ->
            Surface(color = MaterialTheme.colors.background) {
                Column(
                    modifier = Modifier.padding(padding)
                ) {
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
                        chat = chat,
                        onSend = {
                            store.getChat(chatId).thenAccept { chat = it }
                        },
                        isGroupChat,
                        groupEvent,
                    )
                }
            }
        }
    }

    /**
     * Composable responsible for displaying the Contact Field
     */
    @Composable
    fun ContactField(
        toUser: UserInfo,
        chatId: String,
        isGroupChat: Boolean,
        groupEvent: GroupEvent
    ) {
        val context = LocalContext.current
        TopAppBar(
            modifier = Modifier.testTag(CONTACT_FIELD.ROW)
                .clickable {
                    store.removeChatListener(chatId)

                    // if it is a group chat, chatId is the event id
                    if (isGroupChat) {
                        val intent = GroupEventDetailsActivity.getIntent(context, chatId)
                        context.startActivity(intent)
                    } else {
                        // go to the profile of the other user
                        val coachProfileIntent = Intent(context, ProfileActivity::class.java)
                        coachProfileIntent.putExtra("email", toUser.email)
                        // once users can also message other users and not just coaches,
                        // this should be adapted to also work for users
                        coachProfileIntent.putExtra("isViewingCoach", true)
                        context.startActivity(coachProfileIntent)
                    }
                },
            title = {
                Text(
                    text = if (isGroupChat) groupEvent.event.name else "${toUser.firstName} ${toUser.lastName}",
                    modifier = Modifier.testTag(CONTACT_FIELD.LABEL)
                )
            },
            navigationIcon = {
                // Button icon for the    button
                IconButton(
                    onClick = {
                        store.removeChatListener(chatId)
                        // go back to previous activity
                        finish()

                    },
                    modifier = Modifier.testTag(BACK)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
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
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
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
                                    color = MaterialTheme.colors.chatTime,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(5.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onChatTime
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
    fun MessageRow(
        message: Message,
        currentUserEmail: String,
        isFromCurrentUser: Boolean,
        nbParticipants: Int
    ) {
        val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val timeAndUnreadMarkColor = MaterialTheme.colors.onMessageTimeStamp
        val readMarkColor = MaterialTheme.colors.readMessageCheck

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
                            color = if (isFromCurrentUser) MaterialTheme.colors.messageMe else MaterialTheme.colors.messageOther,
                            shape = RoundedCornerShape(
                                10.dp,
                                10.dp,
                                if (isFromCurrentUser) 0.dp else 10.dp,
                                if (isFromCurrentUser) 10.dp else 0.dp
                            )
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                    color = MaterialTheme.colors.onMessage
                )

                // timestamp for message
                Text(
                    text = LocalDateTime.parse(message.timestamp).toLocalTime()
                        .format(timestampFormatter),
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
        chat: Chat,
        onSend: () -> Unit,
        isGroupChat: Boolean,
        groupEvent: GroupEvent,
    ) {
        var message by remember { mutableStateOf("") }

        Surface(
            elevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .testTag(CHAT_FIELD.ROW),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Chat Text Field
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Message") },
                    modifier = Modifier
                        .testTag(CHAT_FIELD.LABEL)
                        .weight(0.9f),
                    shape = CircleShape,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                // Send Button
                if (message.trim().isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            store.getUser(currentUserEmail).thenCompose {
                                store.sendMessage(
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
                                store.getUser(participantMail).thenCompose {
                                    val contact =
                                        if (isGroupChat) groupEvent.groupEventId else chat.id.replace(
                                            participantMail,
                                            ""
                                        )
                                    store.updateUser(
                                        it.copy(
                                            chatContacts = listOf(contact) + it.chatContacts.filter { e -> e != contact })
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .testTag(SEND)
                            .weight(0.1f)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send Message"
                        )
                    }
                }
            }
        }
    }
}

