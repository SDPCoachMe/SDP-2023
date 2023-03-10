package com.github.sdpcoachme

import java.util.concurrent.CompletableFuture

interface Database {

    fun get(key: String): CompletableFuture<Any>
    fun set(key: String, value: Any): CompletableFuture<Void>

}