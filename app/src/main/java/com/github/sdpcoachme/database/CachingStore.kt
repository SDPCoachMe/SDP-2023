package com.github.sdpcoachme.database

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.datastore.core.DataStore
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.first

// todo finir de faire la documentation

/**
 * A caching database that wraps another database
 */
class CachingStore(private val wrappedDatabase: Database,
                   private val dataStore: DataStore<Preferences>,
                   private val context: Context) {

    val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    val CACHED_USERS_KEY = stringPreferencesKey("cached_users")
    val CONTACTS_KEY = stringPreferencesKey("contacts")
    val CHATS_KEY = stringPreferencesKey("chats")
    val CACHED_SCHEDULES_KEY = stringPreferencesKey("cached_schedules")

    private val CACHED_SCHEDULE_WEEKS_AHEAD = 4L
    private val CACHED_SCHEDULE_WEEKS_BEHIND = 4L

    private val cachedUsers = mutableMapOf<String, UserInfo>()
    private val contacts = mutableMapOf<String, List<UserInfo>>()
    private val chats = mutableMapOf<String, Chat>()
    private val cachedSchedules = mutableMapOf<String, List<Event>>()

    private var currentShownMonday = getStartMonday()
    private var minCachedMonday = currentShownMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
    private var maxCachedMonday = currentShownMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

    private var currentEmail: String? = null

    private val gson = Gson()

    private var retrieveData = CompletableFuture.completedFuture(null)
//        if (isOnline()) {
//            retrieveLocalData().thenAccept {
//                Log.d("CachingStore", "Internet available")
//                clearCache()
//            }
//        } else {
//            retrieveLocalData().thenAccept {
//                Log.d("CachingStore", "Internet not available")
//            }
//        }

    init {
        wrappedDatabase.addUsersListeners { users ->
            cachedUsers.clear()
            cachedUsers.putAll(users.associateBy { it.email })
        }
    }

    /**
     * Check whether the user is logged in
     * @return a boolean indicating whether the user is logged in
     */
    fun isLoggedIn(): CompletableFuture<Boolean> {
        return retrieveData.thenApply {
            !currentEmail.isNullOrEmpty()
        }
    }

    /**
     * Retrieves local data from the datastore
     * @return a completable future that completes when the local data has been retrieved
     */
    private fun retrieveLocalData(): CompletableFuture<Void> {
        val localFuture = CompletableFuture<Void>()
        GlobalScope.launch {

            val values = dataStore.data.first()
            currentEmail = values[USER_EMAIL_KEY]

            // Retrieve the Json strings from the datastore
            val serializedUsers = values[CACHED_USERS_KEY]
            val serializedContacts = values[CONTACTS_KEY]
            val serializedChats = values[CHATS_KEY]
            val serializedSchedules = values[CACHED_SCHEDULES_KEY]

            // Deserialize the caching maps from Json and put them in the caching maps
            processRetrievedCache(serializedUsers, cachedUsers)
            processRetrievedCache(serializedContacts, contacts)
            processRetrievedCache(serializedChats, chats)
            processRetrievedCache(serializedSchedules, cachedSchedules)

            localFuture.complete(null)
        }
        return localFuture
    }

    /**
     * Processes a retrieved cache from Json
     * @param jsonString the Json string to process
     * @param cache the cache to put the processed Json into
     */
    private inline fun <reified T> processRetrievedCache(jsonString: String?, cache: MutableMap<String, T>) {
        if (jsonString.isNullOrEmpty()) {
            return
        }
        val type = object : TypeToken<Map<String, T>>() {}.type
        cache.putAll(gson.fromJson(jsonString, type))
    }

    fun storeLocalData(): CompletableFuture<Void> {
        val writeDatastoreFuture = CompletableFuture<Void>()
        GlobalScope.launch {
            dataStore.edit { preferences ->

                // Serialze the caching maps to Json
                val type = object : TypeToken<Map<String, UserInfo>>() {}.type
                val serializedUsers = gson.toJson(cachedUsers, type)
                val serializedContacts = gson.toJson(contacts)
                val serializedChats = gson.toJson(chats)
                val serializedSchedules = gson.toJson(cachedSchedules)

                // Write to datastore
                preferences[USER_EMAIL_KEY] = currentEmail ?: ""
                preferences[CACHED_USERS_KEY] = serializedUsers
                preferences[CONTACTS_KEY] = serializedContacts
                preferences[CHATS_KEY] = serializedChats
                preferences[CACHED_SCHEDULES_KEY] = serializedSchedules

                writeDatastoreFuture.complete(null)
                println("Data was stored")
            }
        }
        return writeDatastoreFuture
    }

    /**
     * Updates the current user
     * @param user the user to update
     * @return a completable future that completes when the user has been updated
     */
    fun updateUser(user: UserInfo): CompletableFuture<Void> {
        return wrappedDatabase.updateUser(user).thenCompose {
            cachedUsers[user.email] = user
            storeLocalData()
        }
    }

    /**
     * Gets the current user
     * @param email the email of the user to get
     * @return a completable future that completes when the user has been retrieved
     */
    fun getUser(email: String): CompletableFuture<UserInfo> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(cachedUsers[email])
        }
        return wrappedDatabase.getUser(email).thenCompose {user ->
            cachedUsers[email] = user
            storeLocalData().thenApply { user }
        }
    }

    /**
     * Gets all users
     * @return a completable future that completes when all users have been retrieved
     */
    fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        if (isOnline()) {
            return wrappedDatabase.getAllUsers().thenCompose { user ->
                cachedUsers.clear()
                cachedUsers.putAll(user.associateBy { it.email })
                storeLocalData().thenApply { user }
            }
        } else {
            return CompletableFuture.completedFuture(cachedUsers.values.toList())
        }
    }

    // todo refactor with userLocation inside CachingStore
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

    fun userExists(email: String): CompletableFuture<Boolean> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(true)
        }
        return wrappedDatabase.userExists(email)
    }

    // Note: to efficiently use caching, we do not use the wrappedDatabase's addEventsToUser method
    fun addEvent(event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        return getCurrentEmail().thenCompose { email ->
            wrappedDatabase.addEvent(email, event, currentWeekMonday).thenApply {
                // Update the cached schedule
                cachedSchedules[email] = cachedSchedules[email]?.plus(event) ?: listOf(event).filter {
                    val start = LocalDateTime.parse(it.start).toLocalDate()
                    val end = LocalDateTime.parse(it.end).toLocalDate()
                    start >= currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
                            && end < currentWeekMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)
                }
                Schedule(cachedSchedules[email] ?: listOf())
            }
        }
    }

    // Note: checks if it is time to prefetch
    /**
     * Gets the schedule for the current user
     * @param currentWeekMonday the monday of the current week
     * @return a completable future that completes when the schedule has been retrieved
     */
    fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val futureEmail = getCurrentEmail()
        currentShownMonday = currentWeekMonday

        return futureEmail.thenCompose { email ->
            if (!cachedSchedules.containsKey(email)) {  // If no cached schedule for that account, we fetch the schedule from the db
                wrappedDatabase.getSchedule(email, currentWeekMonday).thenApply { schedule ->
                    val events = schedule.events.filter {   // We only cache the events that are in the current week or close to it
                        val start = LocalDateTime.parse(it.start).toLocalDate()
                        val end = LocalDateTime.parse(it.end).toLocalDate()
                        start >= minCachedMonday && end <= maxCachedMonday
                    }
                    schedule.copy(events = events).also {   // Update the cache
                        cachedSchedules[email] = it.events
                        storeLocalData()
                    }
                }
            }
            else {
                // If it is time to prefetch (because displayed week is too close to the edge of the cached schedule), we fetch the schedule from the db
                if (currentWeekMonday <= minCachedMonday || currentWeekMonday >= maxCachedMonday) {
                    cachedSchedules.remove(email)

                    // Update the cached schedule's prefetch boundaries
                    minCachedMonday = currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
                    maxCachedMonday = currentWeekMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

                    wrappedDatabase.getSchedule(email, currentWeekMonday).thenApply { schedule ->
                        val events = schedule.events.filter {
                            val start = LocalDateTime.parse(it.start).toLocalDate()
                            val end = LocalDateTime.parse(it.end).toLocalDate()
                            start >= minCachedMonday && end <= maxCachedMonday
                        }
                        schedule.copy(events = events).also {
                            cachedSchedules[email] = it.events  // Update the cache
                            storeLocalData()
                        }
                    }
                }
                else {
                    // If no need to prefetch, we return the cached schedule
                    currentShownMonday = currentWeekMonday
                    CompletableFuture.completedFuture(Schedule(cachedSchedules[email] ?: listOf()))
                }
            }
        }
    }

    /**
     * Get chat contacts for a user
     * @param email the email of the user to get the chat contacts for
     * @return a completable future that completes when the chat contacts have been retrieved
     */
    fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        if (contacts.containsKey(email)) {
            return CompletableFuture.completedFuture(contacts[email])
        }
        return wrappedDatabase.getChatContacts(email).thenApply { it.also { contacts[email] = it } }
    }

    /**
     * Get a chat
     * @param chatId the id of the chat to get
     * @return a completable future that completes when the chat has been retrieved
     */
    fun getChat(chatId: String): CompletableFuture<Chat> {
        if (chats.containsKey(chatId)) {
            return CompletableFuture.completedFuture(chats[chatId]!!)
        }
        return wrappedDatabase.getChat(chatId).thenApply { it.also { chats[chatId] = it } }
    }

    /**
     * Send a message
     * @param chatId the id of the chat to send the message to
     * @param message the message to send
     * @return a completable future that completes when the message has been sent
     */
    fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = chats[chatId]!!.copy(messages = chats[chatId]!!.messages + message)
        }
        return wrappedDatabase.sendMessage(chatId, message) // we only the chat with the new message if the chat is already cached
    }

    /**
     * Mark messages as read
     * @param chatId the id of the chat to mark the messages as read
     * @param email the email of the user to mark the messages as read
     * @return a completable future that completes when the messages have been marked as read
     */
    fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        // Also here, if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = Chat.markOtherUsersMessagesAsRead(
                    chats[chatId]!!,
                    email
                )
        }
        return wrappedDatabase.markMessagesAsRead(chatId, email)
    }

    /**
     * Add a chat listener
     * @param chatId the id of the chat to add the listener to
     * @param onChange the function to call when the chat changes
     */
    fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        val cachingOnChange = { chat: Chat ->
            chats[chatId] = chat
            onChange(chat)
        }
        wrappedDatabase.addChatListener(chatId, cachingOnChange)
    }

    /**
     * Remove a chat listener
     * @param chatId the id of the chat to remove the listener from
     */
    fun removeChatListener(chatId: String) {
        wrappedDatabase.removeChatListener(chatId)
    }


    // No cache here, method just used for testing to fetch from database
    /**
     * Get the FCM token for a user
     * @param email The email of the user to get the FCM token for
     * @return A completable future that completes when the FCM token has been retrieved
     */
    fun getFCMToken(email: String): CompletableFuture<String> {
        return wrappedDatabase.getFCMToken(email)
    }

    /**
     * Set the FCM token for a user
     * @param email The email of the user to set the FCM token for
     * @param token The FCM token to set
     * @return A completable future that completes when the FCM token has been set
     */
    fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        return wrappedDatabase.setFCMToken(email, token)
    }

    /**
     * Get the current email
     * Note: this method needs to return a future as the email is retrieved asynchronously from
     * the local storage (datastore).
     * @return A completable future that completes when the current email has been retrieved
     * @throws IllegalStateException if the current email is null or empty
     */
    fun getCurrentEmail(): CompletableFuture<String> {
        return retrieveData.thenApply {
            if (currentEmail.isNullOrEmpty()) {
                throw IllegalStateException("Current email is null or empty")
            }
            currentEmail
        }
    }

    /**
     * Set the current email
     * @param email The email to set as current
     * @return A completable future that completes when the email has been set
     */
    fun setCurrentEmail(email: String): CompletableFuture<Void> {
        currentEmail = email
        return CompletableFuture.completedFuture(null)
    }



    /**
     * Check if a user is cached
     * Useful for testing
     * @param email The email of the user to check
     * @return True if the user is cached, false otherwise
     */
    fun isCached(email: String): Boolean {
        return cachedUsers.containsKey(email)
    }

    /**
     * Clear the cache.
     * Also useful for testing
     */
    fun clearCache() {
        cachedUsers.clear()
        cachedSchedules.clear()
        contacts.clear()
        chats.clear()
    }

    /**
     * Check if the device is connected to the internet
     * @return True if the device is connected to the internet, false otherwise
     */
    fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    //return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

}