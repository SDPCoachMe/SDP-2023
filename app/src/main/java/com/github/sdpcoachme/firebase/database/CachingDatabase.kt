package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A caching database that wraps another database
 */
class CachingDatabase(private val wrappedDatabase: Database) : Database {
    private val cachedUsers = mutableMapOf<String, UserInfo>()

    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        cachedUsers.remove(user.email)
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

}