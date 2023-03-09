package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

interface Database<V> {

    fun get(key: String): CompletableFuture<V>
    fun set(key: String, value: V): CompletableFuture<Void>

}