package com.github.sdpcoachme.firebase.database

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
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
    private val defaultUserInfo = UserInfo(
        "John", "Doe", "example@email.com",
        "1234567890", "Some location",
        emptyList(), eventList)

    private val db = hashMapOf<String, Any>("accounts" to defaultUserInfo)

    override fun get(key: String): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(db[key])
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        db[key] = value
        return CompletableFuture.completedFuture(null)
    }

    override fun addUser(user: UserInfo): CompletableFuture<Void> {
        return set("accounts", user)
    }

    override fun getUser(email: String): CompletableFuture<Any> {
        return get("accounts")
    }
}