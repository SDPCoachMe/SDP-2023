package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {
    private val defaultUserInfo = mapOf(
        "firstName" to "John",
        "lastName" to "Doe",
        "phone" to "1234567890",
        "location" to "Some location",
        "coach" to false,
        "email" to "example@email.com"
    )

    private val db = hashMapOf<String, Any>("example@email.com" to defaultUserInfo)

    override fun get(key: String): CompletableFuture<Any> {
        if (!db.containsKey(key)) {
            println("Key $key does not exist")
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
        if (user.email == "throw@Exception.com") {
            val error = CompletableFuture<Void>()
            error.completeExceptionally(IllegalArgumentException("Simulated DB error"))
            return error
        }

        val map = mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "phone" to user.phone,
            "location" to user.location,
            "coach" to user.isCoach,
            "email" to user.email
        )

        return set(user.email, map)
    }

    override fun getUser(email: String): CompletableFuture<Any> {
        return get(email)
    }
}