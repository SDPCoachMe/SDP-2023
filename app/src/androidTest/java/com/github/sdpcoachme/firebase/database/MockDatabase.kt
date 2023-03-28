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
    private val eventList = listOf<Event>(Event(
        name = "Google I/O Keynote",
        color = Color(0xFFAFBBF2),
        start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0),
        end = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
    ))
    private val defaultUserInfo = mapOf(
        "firstName" to "John",
        "lastName" to "Doe",
        "phone" to "1234567890",
        "location" to "Some location",
        "coach" to false,
        "email" to "example@email.com",
        "events" to eventList
    )
    private val defautEmail = "example@email.com"
    private val defaultUserInfo = UserInfo(
        "John",
        "Doe",
        defautEmail,
        "1234567890",
        "Some location",
        false,
        listOf())

    private val root = hashMapOf<String, Any>()
    private val accounts = hashMapOf<String, Any>(defautEmail to defaultUserInfo)

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

    //Damian's stuff
    override fun getAccountsRef(): DatabaseReference {
        // Implement this if needed
        return null!!
    }

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {
        // Implement this if needed
        return CompletableFuture()
    }


}