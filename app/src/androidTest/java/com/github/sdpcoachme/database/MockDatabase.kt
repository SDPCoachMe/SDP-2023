package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
open class MockDatabase: Database {
    // TODO: database should be empty by default, and tests should add data to it.
    //  This way, we can make sure each test is independent from the others

    private var chat = Chat(participants = listOf(DEFAULT_EMAIL, TO_EMAIL))
    private var chatId = ""
    private var onChange: (Chat) -> Unit = {}
    private var numberOfAddChatListenerCalls = 0
    private var numberOfRemovedChatListenerCalls = 0

    // TODO: type any is not ideal, needs refactoring
    private var accounts = hashMapOf<String, Any>(DEFAULT_EMAIL to defaultUserInfo)
    private val fcmTokens = hashMapOf<String, String>()
    private var schedules = hashMapOf<String, Schedule>()
    private var groupEvents = hashMapOf<String, GroupEvent>()

    companion object {
        private const val DEFAULT_EMAIL = "example@email.com"
        private val defaultUserInfo = UserInfo(
            "John",
            "Doe",
            DEFAULT_EMAIL,
            "1234567890",
            LAUSANNE,
            false,
            emptyList(),
            emptyList()
        )

        private const val TO_EMAIL = "to@email.com"
        private val toUser = UserInfo(
            "Jane",
            "Doe",
            TO_EMAIL,
            "0987654321",
            LAUSANNE,
            false,
            emptyList(),
            emptyList()
        )

        fun getDefaultEmail(): String {
            return DEFAULT_EMAIL
        }

        fun getToUserEmail(): String {
            return TO_EMAIL
        }

        fun getDefaultUser(): UserInfo {
            return defaultUserInfo
        }

        fun getToUser(): UserInfo {
            return toUser
        }
    }

    fun restoreDefaultChatSetup() {
        chat = Chat(participants = listOf(DEFAULT_EMAIL, TO_EMAIL))
        chatId = ""
        onChange = {}
        numberOfRemovedChatListenerCalls = 0
        numberOfAddChatListenerCalls = 0
    }

    fun restoreDefaultAccountsSetup() {
        accounts = hashMapOf(DEFAULT_EMAIL to defaultUserInfo)
    }

    fun restoreDefaultSchedulesSetup() {
        schedules = hashMapOf()
        groupEvents = hashMapOf()
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
        return getMap(accounts, email)
            .thenApply { it != null }
            .exceptionally { false }
    }

    override fun addEvent(email: String, event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        if (email == "throw@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return getSchedule(email, currentWeekMonday).thenCompose { schedule ->
            val newSchedule = schedule.copy(events = schedule.events + event)
            schedules[email] = newSchedule
            val future = CompletableFuture<Schedule>()
            future.complete(null)
            future
        }
    }

    override fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void> {
        val errorPreventionFuture = CompletableFuture<Void>()

        if (groupEvent.participants.size > groupEvent.maxParticipants) {
            errorPreventionFuture.completeExceptionally(Exception("Group event should not be full, initially"))
        } else if (groupEvent.participants.isEmpty()) {
            errorPreventionFuture.completeExceptionally(Exception("Group event must have at least 2 participants"))
        } else if (LocalDateTime.parse(groupEvent.event.start).isBefore(LocalDateTime.now())) {
            errorPreventionFuture.completeExceptionally(Exception("Group event cannot be in the past"))
        } else {
            groupEvents[groupEvent.groupEventId] = groupEvent
            registerForGroupEvent(groupEvent.organiser, groupEvent.groupEventId).thenCompose {
                errorPreventionFuture.complete(null)
                errorPreventionFuture
            }
        }

        return errorPreventionFuture
    }

    override fun registerForGroupEvent(email: String, groupEventId: String): CompletableFuture<Void> {
        return getGroupEvent(groupEventId, EventOps.getStartMonday()).thenCompose { groupEvent ->
            val hasCapacity = groupEvent.participants.size < groupEvent.maxParticipants
            if (!hasCapacity) {
                val failingFuture = CompletableFuture<Void>()
                failingFuture.completeExceptionally(Exception("Group event is full"))
                failingFuture
            } else {
                val updatedGroupEvent = groupEvent.copy(participants = groupEvent.participants + email)
                groupEvents[groupEventId] = updatedGroupEvent

                getSchedule(email, EventOps.getStartMonday()).thenCompose { s ->
                    val updatedSchedule = s.copy(groupEvents = s.groupEvents + groupEventId)
                    schedules[email] = updatedSchedule
                    CompletableFuture.completedFuture(null)
                }
            }
        }
    }

    override fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        if (email == "throwGetSchedule@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return schedules[email]?.let { CompletableFuture.completedFuture(it) }
            ?: CompletableFuture.completedFuture(Schedule())
    }

    override fun getGroupEvent(groupEventId: String, currentWeekMonday: LocalDate): CompletableFuture<GroupEvent> {
        val future = CompletableFuture<GroupEvent>()
        future.complete(groupEvents[groupEventId] ?: GroupEvent())
        return future
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
            return CompletableFuture.completedFuture(chat)
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        chat = chat.copy(id = chatId, messages = chat.messages + message)
        this.onChange(chat)
        return CompletableFuture.completedFuture(null)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        if (chatId == "run-previous-on-change") {
            val msg = Message(sender = chat.participants[0], content = "test onChange method", timestamp = LocalDateTime.now().toString())
            chat = chat.copy(id = this.chatId , messages = chat.messages + msg)
        } else {
            this.chatId = chatId
            chat = chat.copy(id = chatId)
            this.onChange = onChange
            numberOfAddChatListenerCalls++
        }
        this.onChange(chat)
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        return CompletableFuture.completedFuture(listOf(toUser))
    }


    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        chat = Chat.markOtherUsersMessagesAsRead(chat, email)

        return CompletableFuture.completedFuture(null)
    }

    override fun removeChatListener(chatId: String) {
        numberOfRemovedChatListenerCalls++
    }

    override fun addUsersListeners(onChange: (List<UserInfo>) -> Unit) {
        // Not necessary
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
}