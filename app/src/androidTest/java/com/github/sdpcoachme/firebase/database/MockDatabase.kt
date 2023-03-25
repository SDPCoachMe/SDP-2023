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
            "email" to user.email,
            "events" to user.events
        )

        return set(user.email, map)
    }

    override fun getUser(email: String): CompletableFuture<Any> {
        return get(email)
    }

    override fun getAccountsRef(): DatabaseReference {
        // Implement this if needed
        return null!!
    }

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {
        // Implement this if needed
        return CompletableFuture()
    }


}