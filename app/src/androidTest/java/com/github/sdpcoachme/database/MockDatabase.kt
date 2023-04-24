package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {

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
    val toUser = UserInfo(
        "Jane",
        "Doe",
        toEmail,
        "0987654321",
        LAUSANNE,
        false,
        emptyList(),
        emptyList()
    )

    private var chat = Chat(participants = listOf(defaultEmail, toEmail))
    private var chatId = ""
    private var onChange: (Chat) -> Unit = {}

    fun restoreDefaultChatSetup() {
        chat = Chat(participants = listOf(defaultEmail, toEmail))
        chatId = ""
        onChange = {}
    }

    fun restoreDefaultAccountsSetup() {
        accounts = hashMapOf(defaultEmail to defaultUserInfo)
    }


    // TODO: type any is not ideal, needs refactoring
    private var accounts = hashMapOf<String, Any>(defaultEmail to defaultUserInfo)

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

    override fun addEventsToUser(email: String, events: List<Event>): CompletableFuture<Void> {
        return getUser(email).thenCompose { user ->
            val newUserInfo = user.copy(events = user.events + events)
            setMap(accounts, email, newUserInfo)
        }
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
        }
        this.onChange(chat)
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        return CompletableFuture.completedFuture(listOf(toUser))
    }


    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        chat = chat.copy(messages = chat.messages.map { message ->
            if (message.sender == email) {
                message.copy(readByRecipient = true)
            } else {
                message
            }
        })

        return CompletableFuture.completedFuture(null)
    }

    override fun removeChatListener(chatId: String) {
        // no need to do anything
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