package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

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