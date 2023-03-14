package com.github.sdpcoachme.database

import java.util.concurrent.CompletableFuture

/**
 * A mock database class
 */
class MockDatabase: Database {

    private val db = hashMapOf<String, Any>()

    override fun get(key: String): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(db[key])
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        db[key] = value
        return CompletableFuture.completedFuture(null)
    }
}