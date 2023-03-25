package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {
    private val defautEmail = "example@email.com"
    private val defaultUserInfo = UserInfo(
        "John", "Doe", defautEmail,
        "1234567890", "Some location",
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
        return CompletableFuture.completedFuture(map[key])
    }
}