package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

class MockDatabase<V>: Database<V> {

    private val db = hashMapOf<String, V>()


    override fun get(key: String): CompletableFuture<V> {
        return CompletableFuture.completedFuture(db[key])
    }

    override fun set(key: String, value: V) {
        db[key] = value
    }
}