package com.github.sdpcoachme.database

import com.github.sdpcoachme.UserInfo
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

    /**
     * Add a user to the database
     * @param user The user to add
     * @return A future that will complete when the user has been added
     */
    fun addUser(user: UserInfo): CompletableFuture<Void>

    /**
     * Get a user from the database
     * @param user The user to get
     * @return A future that will complete when the user has been gotten
     */
    fun getUser(user: UserInfo): CompletableFuture<Any>

}