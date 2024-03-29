package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
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
     * Check if a user exists in the database
     * @param email The email of the user to check
     * @return A future that will complete when the user has been checked. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun userExists(email: String): CompletableFuture<Boolean>

    /**
     * Get the group event from the database
     * @param groupEventId The id of the group event to get
     * @return A future that will complete with the group event. If the group event does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent>

    /**
     * Get all group events from the database
     * @return A future that will complete with a list of all group events in the database
     */
    fun getAllGroupEvents(): CompletableFuture<List<GroupEvent>>

    /**
     * Updates the value of a group event in the database (adds it if it does not exist)
     * @param groupEvent The group event to update
     * @return A future that will complete when the group event has been updated.
     */
    fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void>

    /**
     * Add the group event to the schedule of the given user
     * @param email The email of the user whose schedule should be updated
     * @param groupEventId The id of the group event to add
     * @return A future with currently stored schedule that will complete when the group event has been added.
     */
    fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule>

    /**
     * Add an event to the schedule of a given user
     * @param event The event to add
     * @param email The email of the user whose schedule will be updated
     * @return A future with currently stored schedule that will complete when the event has been added.
     */
    fun addEventToSchedule(email: String, event: Event): CompletableFuture<Schedule>

    /**
     * Get the schedule from the database
     * @param email The email of the user to get the schedule for
     * @return A future that will complete with the schedule. If the user does not exist,
     * the future will complete exceptionally with a NoSuchKeyException.
     */
    fun getSchedule(email: String): CompletableFuture<Schedule>

    /**
     * Get the contact row info for the given user
     * This will be used to display the user's contacts in the UI
     * similar to other messaging services such as WhatsApp:
     * The name of the chat / recipient and the last message will be displayed
     *
     * @param email The email of the user whose contacts should be retrieved
     * @return A future that will complete with the contact row info
     */
    fun getContactRowInfos(email: String): CompletableFuture<List<ContactRowInfo>>

    /**
     * Get chat with the given id from the database
     *
     * @param chatId The id of the chat to get
     * @return A future that will complete with the chat
     */
    fun getChat(chatId: String): CompletableFuture<Chat>

    /**
     * Update / create chat with the following participants
     * If the chat already exists, it will be updated with the new participants
     * If the chat does not exist, it will be created with the given participants
     *
     * @param chatId The id of the chat
     * @param participants The participants of the chat
     * @return A future that will complete when the user has been added
     */
    fun updateChatParticipants(chatId: String, participants: List<String>): CompletableFuture<Void>

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