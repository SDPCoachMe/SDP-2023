package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
open class MockDatabase: Database {

    // TODO: database should be empty by default, and tests should add data to it.
    //  This way, we can make sure each test is independent from the others
    private val defaultEmail = "example@email.com"
    private val defaultUserInfo = UserInfo(
        "John",
        "Doe",
        defaultEmail,
        "1234567890",
        LAUSANNE,
        false,
        emptyList(),
        emptyList()
    )
    private var currEmail = ""

    private val toEmail = "to@email.com"
    private val toUser = UserInfoSamples.COACH_1

    private val toUser2 = UserInfoSamples.NON_COACH_1

    private var chat = Chat(participants = listOf(defaultEmail, toEmail))
    private var chatId = ""
    private var onChange: (Chat) -> Unit = {}
    private var numberOfAddChatListenerCalls = 0
    private var numberOfRemovedChatListenerCalls = 0


    private var groupChat = Chat(participants = listOf(defaultEmail, toEmail, toUser2.email))
    private var groupChatId = ""
    private var groupChatTitle = "Group Chat"
    private var groupChatOnChange: (Chat) -> Unit = {}
    private var groupChatNumberOfAddChatListenerCalls = 0
    private var groupChatNumberOfRemovedChatListenerCalls = 0

    // TODO: type any is not ideal, needs refactoring
    private var accounts = hashMapOf<String, Any>(defaultEmail to defaultUserInfo)
    private val fcmTokens = hashMapOf<String, String>()
    private var schedules = hashMapOf<String, Schedule>()

    fun restoreDefaultChatSetup() {
        chat = Chat(participants = listOf(defaultEmail, toEmail))
        chatId = ""
        onChange = {}
        numberOfRemovedChatListenerCalls = 0
        numberOfAddChatListenerCalls = 0

        groupChat = Chat(participants = listOf(defaultEmail, toEmail, toUser2.email))
        groupChatId = ""
        groupChatTitle = "Group Chat"
        groupChatOnChange = {}
        groupChatNumberOfAddChatListenerCalls = 0
        groupChatNumberOfRemovedChatListenerCalls = 0
    }

    fun restoreDefaultAccountsSetup() {
        accounts = hashMapOf(defaultEmail to defaultUserInfo)
    }

    fun restoreDefaultSchedulesSetup() {
        schedules = hashMapOf()
    }


    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        if (user.email == "throw@Exception.com") {
            val error = CompletableFuture<Void>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return setMap(accounts, user.email, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        if (email == "throwGet@Exception.com") {
            val error = CompletableFuture<UserInfo>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return getMap(accounts, email).thenApply { it as UserInfo }

    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        val future = CompletableFuture<List<UserInfo>>()
        future.complete(accounts.values.map { it as UserInfo })
        return future
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        return getMap(accounts, email).thenApply { it != null }
    }

    override fun addEvent(event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        if (currEmail == "throw@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return getSchedule(currentWeekMonday).thenCompose { schedule ->
            val newSchedule = schedule.copy(events = schedule.events + event)
            schedules[currEmail] = newSchedule
            val future = CompletableFuture<Schedule>()
            future.complete(null)
            future
        }
    }

    override fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        if (currEmail == "throwGetSchedule@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return schedules[currEmail]?.let { CompletableFuture.completedFuture(it) }
            ?: CompletableFuture.completedFuture(Schedule(emptyList()))
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        if (chatId.startsWith("@@event")) {
            return CompletableFuture.completedFuture(groupChat)
        }
        return CompletableFuture.completedFuture(chat)
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        if (chatId.startsWith("@@event")) {
            groupChat = groupChat.copy(id = chatId, messages = groupChat.messages + message)
            this.onChange(groupChat)
        } else {
            chat = chat.copy(id = chatId, messages = chat.messages + message)
            this.onChange(chat)
        }


        return CompletableFuture.completedFuture(null)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        if (chatId == "run-previous-on-change") {
            val msg = Message(sender = chat.participants[0], content = "test onChange method", timestamp = LocalDateTime.now().toString())
            chat = chat.copy(id = this.chatId , messages = chat.messages + msg)
        } else {
            if (chatId.startsWith("@@event")) {
                groupChatId = chatId
                groupChat = groupChat.copy(id = chatId)
                groupChatOnChange = onChange
                groupChatNumberOfAddChatListenerCalls++
                this.onChange(groupChat)
                return
            } else {
                this.chatId = chatId
                chat = chat.copy(id = chatId)
                this.onChange = onChange
                numberOfAddChatListenerCalls++
            }
        }
        this.onChange(chat)
    }

    override fun getContactRowInfo(email: String): CompletableFuture<List<ContactRowInfo>> {
        val id = if (email < toEmail) email+toUser.email else toEmail+email
        return CompletableFuture.completedFuture(listOf(
            ContactRowInfo(id, toUser.firstName + " " + toUser.lastName, Message()),
            ContactRowInfo(groupChatId, "Group Chat", Message(), true)
        ))
    }

    override fun updateChatParticipants(
        chatId: String,
        participants: List<String>
    ): CompletableFuture<Void> {
        if (chatId.startsWith("@@event")) { // only group chats can be updated
            groupChat = groupChat.copy(participants = participants)
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun addGroupEvent(
        groupEvent: GroupEvent,
        currentWeekMonday: LocalDate
    ): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    override fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    override fun getGroupEvent(
        groupEventId: String,
        currentWeekMonday: LocalDate
    ): CompletableFuture<GroupEvent> {
        TODO("Not yet implemented")
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        if (chatId.startsWith("@@event")) {
            groupChat = Chat.markOtherUsersMessagesAsRead(groupChat, email)
        } else {
            chat = Chat.markOtherUsersMessagesAsRead(chat, email)
        }

        return CompletableFuture.completedFuture(null)
    }

    override fun removeChatListener(chatId: String) {
        numberOfRemovedChatListenerCalls++
    }

    fun numberOfRemovedChatListenerCalls(): Int {
        return numberOfRemovedChatListenerCalls
    }

    fun numberOfAddChatListenerCalls(): Int {
        return numberOfAddChatListenerCalls
    }

    override fun getFCMToken(email: String): CompletableFuture<String> {
        fcmTokens[email]?.let {
            return CompletableFuture.completedFuture(it)
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        fcmTokens[email] = token
        return CompletableFuture.completedFuture(null)
    }

    private fun setMap(map: MutableMap<String, Any>, key: String, value: Any): CompletableFuture<Void> {
        map[key] = value
        return CompletableFuture.completedFuture(null)
    }

    private fun getMap(map: MutableMap<String, Any>, key: String): CompletableFuture<Any> {
        val future = CompletableFuture<Any>()
        val value = map[key]
        if (value == null) {
            val exception = "Key $key does not exist"
            println(exception)
            future.completeExceptionally(Database.NoSuchKeyException(exception))
        } else
            future.complete(value)
        return future
    }

    override fun getCurrentEmail(): String {
        return currEmail
    }

    override fun setCurrentEmail(email: String) {
        currEmail = email
    }
}