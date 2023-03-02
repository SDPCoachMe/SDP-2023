package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

interface Database<K, V> {

    fun get(key: K): CompletableFuture<V>
    fun set(key: K, value: V)

}