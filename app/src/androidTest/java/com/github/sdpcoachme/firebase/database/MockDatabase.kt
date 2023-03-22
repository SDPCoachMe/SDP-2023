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
        listOf())

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

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        return get("accounts") // todo ça ne va pas
    }
}