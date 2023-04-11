package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import java.util.concurrent.CompletableFuture

/**
 * A database interface
 */
interface Database {

    /**
     * Add a user to the database
     * @param user The user to add
     * @return A future that will complete when the user has been added
     */
    // TODO change the name since this can also be used to update a user
    fun addUser(user: UserInfo): CompletableFuture<Void>

    /**
     * Get a user from the database
     * @param email The email of the user to get
     * @return A future that will complete when the user has been gotten
     */
    fun getUser(email: String): CompletableFuture<UserInfo>

    /**
     * Get all users from the database
     * @return A future that will complete with a list of all users in the database
     */
    fun getAllUsers(): CompletableFuture<List<UserInfo>>

    /**
     * Check if a user exists in the database
     * @param email The email of the user to check
     * @return A future that will complete when the user has been checked
     */
    fun userExists(email: String): CompletableFuture<Boolean>

    /**
     * Add events to the database
     * @param email The email of the user to add the events to
     * @param events The events to add
     * @return A future that will complete when the events have been added
     */
    fun addEventsToUser(email: String, events: List<Event>): CompletableFuture<Void>

    /**
     * Get the current user's email
     * @return The current user's email
     */
    fun getCurrentEmail(): String

    /**
     * Set the current user's email
     * @param email The email to set
     */
    fun setCurrentEmail(email: String)

}