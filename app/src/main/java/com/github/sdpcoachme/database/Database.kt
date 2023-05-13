package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.GroupEvent
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
     * Add event to the database
     * @param event The event to add
     * @param currentWeekMonday The monday of the current week
     * @return A future with currently stored schedule that will complete when the event has been added.
     * @param email The email of the user to add the events for
     * @return A future with currently stored schedule that will complete when the events have been added.
     */
    fun addEvent(email: String, event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule>

    /**
     * Add group event to the database
     * @param groupEvent The group event to add
     * @param currentWeekMonday The monday of the current week
     * @return A future that will complete when the group event has been added.
     */
    fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void>

    /**
     * Add new participant to the group event
     * @param email The email of the user to add as a participant
     * @param groupEventId The id of the group event to add the participant to
     * @return A future that will complete when the participant has been added.
     */
    fun registerForGroupEvent(email: String, groupEventId: String): CompletableFuture<Void>

    /**
     * Get the schedule from the database
     * @param currentWeekMonday The monday of the current week
     * @param email The email of the user to get the schedule for
     * @return A future that will complete with the schedule. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule>

    /**
     * Get the group event from the database
     * @param groupEventId The id of the group event to get
     * @param currentWeekMonday The monday of the current week
     * @return A future that will complete with the group event. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getGroupEvent(groupEventId: String, currentWeekMonday: LocalDate): CompletableFuture<GroupEvent>

    /**
     * Get the chat contacts for the given user
     * @param email The email of the user to get the chat contacts for
     * @return A future that will complete with the chat contacts
     */
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

    /**
     * Mark all messages in the chat as read by the current user
     *
     * @param chatId The id of the chat
     * @return A future that will complete when the messages have been marked as read
     */
    fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void>

    /**
     * Add a listener to the chat with the given id
     *
     * @param chatId The id of the chat to listen to
     * @param onChange The function to call when the chat changes
     */
    fun addChatListener(chatId: String, onChange: (Chat) -> Unit)

    /**
     * Remove the listener for the chat with the given id
     *
     * @param chatId The id of the chat to stop listening to
     */
    fun removeChatListener(chatId: String)

    /**
     * Add a listener to the users
     *
     * @param onChange The function to call when the users change
     */
    fun addUsersListeners(onChange: (List<UserInfo>) -> Unit)

    /**
     * Get the FCM token data class for the given user
     *
     * @param email The email of the user to get the token for
     * @return A future that will complete with the token
     */
    fun getFCMToken(email: String): CompletableFuture<String>

    /**
     * Set the FCM token for the given user
     *
     * @param email The email of the user to set the token for
     * @param token The token to set
     * @return A future that will complete when the token has been set
     */
    fun setFCMToken(email: String, token: String): CompletableFuture<Void>

    // Used to handle database errors
    class NoSuchKeyException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
}