package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.AddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_2
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_3
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
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

    private var groupChat = Chat()

    // TODO: type any is not ideal, needs refactoring
    private var accounts = hashMapOf<String, Any>(
        DEFAULT_EMAIL to defaultUserInfo,
        COACH_1.email to COACH_1,
        COACH_2.email to COACH_2,
        COACH_3.email to COACH_3,
    )
    private val fcmTokens = hashMapOf<String, String>()
    private var schedules = hashMapOf<String, Schedule>()
    private var groupEvents = hashMapOf<String, GroupEvent>()

    companion object {
        private const val DEFAULT_EMAIL = "example@email.com"
        private val defaultUserInfo = UserInfo(
            "Firstname",
            "Secondname",
            DEFAULT_EMAIL,
            "1234567890",
            LAUSANNE,
            false,
            emptyList(),
            emptyList()
        )

        private val TO_EMAIL = COACH_1.email
        private val toUser = COACH_1

        // Those functions are going to be used in the CacheStore tests
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

    override fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent> {
        val future = CompletableFuture<GroupEvent>()
        future.complete(groupEvents[groupEventId] ?: GroupEvent())
        return future
    }

    override fun getAllGroupEvents(): CompletableFuture<List<GroupEvent>> {
        return CompletableFuture.completedFuture(groupEvents.values.toList())
    }

    override fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
        groupEvents[groupEvent.groupEventId] = groupEvent
        return CompletableFuture.completedFuture(null)
    }

    override fun addEventToSchedule(email: String, event: Event): CompletableFuture<Schedule> {
        if (email == "throw@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return getSchedule(email).thenCompose { schedule ->
            val newSchedule = schedule.copy(events = schedule.events + event)
            schedules[email] = newSchedule
            CompletableFuture.completedFuture(newSchedule)
        }
    }

    override fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule> {
        return getSchedule(email).thenCompose { s ->
            val updatedSchedule = s.copy(groupEvents = s.groupEvents + groupEventId)
            schedules[email] = updatedSchedule
            CompletableFuture.completedFuture(updatedSchedule)
        }
    }

    override fun getSchedule(email: String): CompletableFuture<Schedule> {
        if (email == "throwGetSchedule@Exception.com") {
            val error = CompletableFuture<Schedule>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }
        return schedules[email]?.let { CompletableFuture.completedFuture(it) }
            ?: CompletableFuture.completedFuture(Schedule())
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
                groupChat = groupChat.copy(id = chatId)
                this.onChange(groupChat)
                return
            } else {
                this.chatId = chatId
                chat = chat.copy(id = chatId)
                this.onChange = onChange
            }
        }
        this.onChange(chat)
    }

    var chatTitle = "Group Chat"
    override fun getContactRowInfos(email: String): CompletableFuture<List<ContactRowInfo>> {
        val id = if (email < toUser.email) email+toUser.email else toUser.email+email
        return CompletableFuture.completedFuture(listOf(
            ContactRowInfo(id, toUser.firstName + " " + toUser.lastName, if (chat.messages.isEmpty()) Message() else chat.messages.last()),
            ContactRowInfo(groupChat.id, chatTitle, if (groupChat.messages.isEmpty()) Message() else groupChat.messages.last(), true)
        ))
    }

    override fun updateChatParticipants(
        chatId: String,
        participants: List<String>
    ): CompletableFuture<Void> {
        if (chatId.startsWith("@@event")) { // only group chats can be updated
            groupChat = groupChat.copy(id = chatId, participants = participants)
            chatTitle = groupEvents[chatId]?.event?.name ?: "Group Chat"
        }
        return CompletableFuture.completedFuture(null)
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
        // Not necessary
    }

    override fun addUsersListeners(onChange: (List<UserInfo>) -> Unit) {
        // Not necessary
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