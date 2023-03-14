package com.github.sdpcoachme.database

import java.util.concurrent.CompletableFuture

/**
 * A database interface
 */
interface Database {


    /**
     * Get a value from the database
     * @param key The key of the value to get
     * @return A future that will complete with the value
     */
    fun get(key: String): CompletableFuture<Any>

    /**
     * Set a value in the database
     * @param key The key of the value to set
     * @param value The value to set
     * @return A future that will complete when the value has been set
     */
    fun set(key: String, value: Any): CompletableFuture<Void>

}