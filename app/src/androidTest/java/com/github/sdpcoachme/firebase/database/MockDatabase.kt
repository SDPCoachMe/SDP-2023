package com.github.sdpcoachme.firebase.database

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.google.firebase.database.DatabaseReference
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {

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
        return getMap(accounts, email).thenApply { it as UserInfo }

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

    fun getDefaultEmail(): String {
        return defaultEmail
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
}