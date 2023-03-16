package com.github.sdpcoachme.database

import com.github.sdpcoachme.ListSport
import com.github.sdpcoachme.firebase.database.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {
    private val defaultUserInfo = UserInfo(
        "John", "Doe", "example@email.com",
        "1234567890", "Some location",
        listOf(ListSport("Some sport", true), ListSport("Some other sport", false)))

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