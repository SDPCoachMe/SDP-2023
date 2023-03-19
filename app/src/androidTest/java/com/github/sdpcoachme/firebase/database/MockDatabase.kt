package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {
    private val defaultUserInfo = UserInfo(
        "John", "Doe", "example@email.com",
        "1234567890", "Some location",
        false, listOf())

    private val db = hashMapOf<String, Any>("example@email.com" to defaultUserInfo)

    override fun get(key: String): CompletableFuture<Any> {
        if (!db.containsKey(key)) {
            val error = CompletableFuture<Any>()
            error.completeExceptionally(NoSuchElementException("Key $key does not exist"))
            return error
        }

        return CompletableFuture.completedFuture(db[key])
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        db[key] = value
        return CompletableFuture.completedFuture(null)
    }

    override fun addUser(user: UserInfo): CompletableFuture<Void> {
        return set(user.email, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        println("Getting user $email")
        return get(email).thenApply { user -> user as UserInfo }
    }
}