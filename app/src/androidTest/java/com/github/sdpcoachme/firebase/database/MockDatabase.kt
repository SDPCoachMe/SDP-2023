package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase() : Database {
    var email: String = ""
    override var currentUserEmail: String
        get() = email
        set(value) {email = value}

    private val defaultEmail = "example@email.com"
    private val defaultUserInfo = UserInfo(
        "John",
        "Doe",
        defaultEmail,
        "1234567890",
        "Some location",
        false,
        emptyList(),
        emptyList()
    )

    private val toEmail = "to@email.com"
    val toUser = UserInfo(
        "Jane",
        "Doe",
        toEmail,
        "0987654321",
        "Some location",
        false,
        emptyList(),
        emptyList()
    )

    private val chatMap = hashMapOf<String, Chat>()
    private var onChange: (Chat) -> Unit = {}


    // TODO: type any is not ideal, needs refactoring
    private val root = hashMapOf<String, Any>()
    private val accounts = hashMapOf<String, Any>(defaultEmail to defaultUserInfo)


    override fun get(key: String): CompletableFuture<Any> {
        return getMap(root, key)
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        return setMap(root, key, value)
    }

    override fun addUser(user: UserInfo): CompletableFuture<Void> {
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

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {
        return getUser(email).thenCompose { user ->
            val newUserInfo = user.copy(events = user.events + events)
            setMap(accounts, email, newUserInfo)
        }
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        // TODO: add exception case
        return CompletableFuture.completedFuture(chatMap.getOrDefault(chatId, Chat()))
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // TODO: add exception case
        val chat = chatMap.getOrDefault(chatId, Chat())
        chatMap[chatId] = chat.copy(id = chatId, messages = chat.messages + message)
        onChange(chat)
        return CompletableFuture.completedFuture(null)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        // TODO: implement
        this.onChange = onChange
        val chat = chatMap.getOrDefault(chatId, Chat())

        onChange(chat)
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        // TODO: add exception case (and maybe more users)
        return CompletableFuture.completedFuture(listOf(toUser))
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
            future.completeExceptionally(NoSuchKeyException(exception))
        } else
            future.complete(value)
        return future
    }

    init {
        this.currentUserEmail = ""
    }
}