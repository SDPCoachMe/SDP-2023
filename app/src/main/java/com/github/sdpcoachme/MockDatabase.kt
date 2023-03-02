package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

class MockDatabase<K, V>: Database<K, V> {

    private val db = hashMapOf<K, V>()


    override fun get(key: K): CompletableFuture<V> {
        return CompletableFuture.completedFuture(db[key])
    }

    override fun set(key: K, value: V) {
        db[key] = value
    }
}