package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Schedule
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

/**
 * A database interface
 */
interface Database {

    /**
     * Update a user's info in the database
     * @param user The user to update
     * @return A future that will complete when the user info has been updated.
     */
    fun updateUser(user: UserInfo): CompletableFuture<Void>

    /**
     * Get a user from the database
     * @param email The email of the user to get
     * @return A future that will complete when the user has been gotten. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getUser(email: String): CompletableFuture<UserInfo>

    /**
     * Get all users from the database
     * @return A future that will complete with a list of all users in the database
     */
    fun getAllUsers(): CompletableFuture<List<UserInfo>>

    /**
     * Get all users from the database sorted by distance from a given location
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return A future that will complete with a list of all users in the database sorted by distance
     */
    fun getAllUsersByNearest(latitude: Double, longitude: Double): CompletableFuture<List<UserInfo>> {
        return getAllUsers().thenApply { users ->
            users.sortedBy { user ->
                val userLatitude = user.address.latitude
                val userLongitude = user.address.longitude
                val distance = SphericalUtil.computeDistanceBetween(
                    LatLng(latitude, longitude),
                    LatLng(userLatitude, userLongitude)
                )
                distance
            }
        }
    }

    /**
     * Check if a user exists in the database
     * @param email The email of the user to check
     * @return A future that will complete when the user has been checked. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun userExists(email: String): CompletableFuture<Boolean>

    /**
     * Add events to the database
     * @param events The events to add
     * @param currentWeekMonday The monday of the current week
     * @return A future with currently stored schedule that will complete when the events have been added.
     */
    fun addEvents(events: List<Event>, currentWeekMonday: LocalDate): CompletableFuture<Schedule>


    /**
     * Get the schedule from the database
     * @param currentWeekMonday The monday of the current week
     * @return A future that will complete with the schedule. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule>

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

    // Used to handle database errors
    class NoSuchKeyException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    fun getChatContacts(email: String): CompletableFuture<List<UserInfo>>
    /**
     * Get chat with the given id from the database
     *
     * @param chatId The id of the chat to get
     * @return A future that will complete with the chat
     */
    fun getChat(chatId: String): CompletableFuture<Chat>

    /**
     * Place the new message into the database
     *
     * @param chatId The id of the chat to add the message to
     * @param message The message to add
     * @return A future that will complete when the message has been added
     */
    fun sendMessage(chatId: String, message: Message): CompletableFuture<Void>

    fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void>

    fun addChatListener(chatId: String, onChange: (Chat) -> Unit)
    fun removeChatListener(chatId: String)
}