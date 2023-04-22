package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import java.util.concurrent.CompletableFuture

/**
 * A caching database that wraps another database
 */
class CachingDatabase(private val wrappedDatabase: Database) : Database {
    private val cachedUsers = mutableMapOf<String, UserInfo>()
    private val cachedTokens = mutableMapOf<String, String>()

    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        return wrappedDatabase.updateUser(user).thenAccept { cachedUsers[user.email] = user }
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(cachedUsers[email])
        }
        return wrappedDatabase.getUser(email).thenApply {
            it.also { cachedUsers[email] = it }
        }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return wrappedDatabase.getAllUsers().thenApply {
            it.also {
                cachedUsers.clear()
                cachedUsers.putAll(it.associateBy { it.email }) }
        }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(true)
        }
        return wrappedDatabase.userExists(email)
    }

    // Note: to efficiently use caching, we do not use the wrappedDatabase's addEventsToUser method
    override fun addEventsToUser(email: String, events: List<Event>): CompletableFuture<Void> {
        return getUser(email).thenCompose {
            val updatedUserInfo = it.copy(events = it.events + events)
            updateUser(updatedUserInfo)
        }
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        // TODO implement in next sprint
        return wrappedDatabase.getChatContacts(email)
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        // TODO implement in next sprint
        return wrappedDatabase.getChat(chatId)
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // TODO implement in next sprint
        return wrappedDatabase.sendMessage(chatId, message)
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        // TODO implement in next sprint
        return wrappedDatabase.markMessagesAsRead(chatId, email)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        // TODO implement in next sprint (adapt onChange to change this here and then call the passed onChange!)
        wrappedDatabase.addChatListener(chatId, onChange)
    }

    override fun removeChatListener(chatId: String) {
        // TODO implement in next sprint
        wrappedDatabase.removeChatListener(chatId)
    }

    override fun getFCMToken(email: String): CompletableFuture<String> {
        if (cachedTokens.containsKey(email)) {
            return CompletableFuture.completedFuture(cachedTokens[email])
        }
        return wrappedDatabase.getFCMToken(email)
    }

    override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        cachedTokens[email] = token
        return wrappedDatabase.setFCMToken(email, token)
    }

    override fun getCurrentEmail(): String {
        return wrappedDatabase.getCurrentEmail()
    }

    override fun setCurrentEmail(email: String) {
        wrappedDatabase.setCurrentEmail(email)
    }

    /**
     * Check if a user is cached
     * Useful for testing
     * @param email The email of the user to check
     * @return True if the user is cached, false otherwise
     */
    fun isCached(email: String): Boolean {
        return cachedUsers.containsKey(email)
    }

    /**
     * Clear the cache.
     * Also useful for testing
     */
    fun clearCache() {
        cachedUsers.clear()
    }
}