package com.github.sdpcoachme.database

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

// todo finir de faire la documentation

/**
 * A caching database that wraps another database
 */
class CachingStore(private val wrappedDatabase: Database,
                   private val dataStore: DataStore<Preferences>,
                   context: Context) {

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

    private var cachedSchedule = Schedule()
    private var currentShownMonday = getStartMonday()
    private var minCachedMonday = currentShownMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
    private var maxCachedMonday = currentShownMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

    private var currentEmail: String? = null

    private var retrieveData =
        if (isOnline(context)) {
            retrieveLocalData().thenCompose {
                Log.d("CachingStore", "Internet available")
                retrieveRemoteData()
            }
        } else {
            retrieveLocalData().thenAccept {
                Log.d("CachingStore", "Internet not available")
            }
        }

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

            /*
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

             */

            localFuture.complete(null)
        }
        return localFuture
    }

    /**
     * Processes a retrieved cache from Json
     * @param jsonString the Json string to process
     * @param cache the cache to put the processed Json into
     */
    private fun <T> processRetrievedCache(jsonString: String?, cache: MutableMap<String, T>) {
        if (jsonString.isNullOrEmpty()) {
            return
        }
        val gson = Gson()
        val type = object : TypeToken<Map<String, T>>() {}.type
        cache.putAll(gson.fromJson(jsonString, type))
    }

    /**
     * Retrieve remote data from the database
     * @return a completable future that completes when the remote data has been retrieved
     */
    private fun retrieveRemoteData(): CompletableFuture<Void> {
        return CompletableFuture.allOf(
            // todo if we could get by nearest here it would probably work to retrieve nearby coaches in offline mode
            //getAllUsers()
        )
    }

    fun storeLocalData(): CompletableFuture<Void> {
        val writeDatastoreFuture = CompletableFuture<Void>()
        GlobalScope.launch {
            /*
            dataStore.edit { preferences ->
                val gson = Gson()

                // Serialze the caching maps to Json
                val serializedUsers = gson.toJson(cachedUsers)
                val serializedContacts = gson.toJson(contacts)
                val serializedChats = gson.toJson(chats)
                val serializedSchedules = gson.toJson(cachedSchedules)

                // Write to datastore
                preferences[USER_EMAIL_KEY] = currentEmail ?: ""
                preferences[CACHED_USERS_KEY] = serializedUsers
                preferences[CONTACTS_KEY] = serializedContacts
                preferences[CHATS_KEY] = serializedChats
                preferences[CACHED_SCHEDULES_KEY] = serializedSchedules

             */

                writeDatastoreFuture.complete(null)
                println("Data was stored")
            //}
        }
        return writeDatastoreFuture
    }


    fun updateUser(user: UserInfo): CompletableFuture<Void> {
        return wrappedDatabase.updateUser(user).thenAccept {
            cachedUsers[user.email] = user
            storeLocalData()
        }
    }

    fun getUser(email: String): CompletableFuture<UserInfo> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(cachedUsers[email])
        }
        return wrappedDatabase.getUser(email).thenApply {
            it.also { cachedUsers[email] = it
            storeLocalData()
            }
        }
    }

    fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return wrappedDatabase.getAllUsers().thenApply {
            it.also {
                cachedUsers.clear()
                cachedUsers.putAll(it.associateBy { it.email }) }
        }
    }

    // todo refactor with userLocation inside CachingStore
    fun getAllUsersByNearest(latitude: Double, longitude: Double): CompletableFuture<List<UserInfo>> {
        return wrappedDatabase.getAllUsersByNearest(latitude, longitude).thenApply {
            it.also {
                cachedUsers.clear()
                cachedUsers.putAll(it.associateBy { it.email })
                storeLocalData()
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
                val start = LocalDateTime.parse(event.start).toLocalDate()
                val end = LocalDateTime.parse(event.end).toLocalDate()
                if (start >= minCachedMonday && end < maxCachedMonday) {
                    cachedSchedule = cachedSchedule.copy(events = cachedSchedule.events.plus(event))
                }

                cachedSchedule
            }
        }
    }

    fun addGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
        return wrappedDatabase.addGroupEvent(groupEvent)
    }

    fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
        return getCurrentEmail().thenCompose { email ->
            wrappedDatabase.registerForGroupEvent(email, groupEventId).thenCompose {
                wrappedDatabase.getSchedule(email, currentShownMonday)
            }.thenAccept { schedule ->
                cachedSchedule = schedule
            }
        }
    }

    private fun fetchGroupEvents(schedule: Schedule, currentWeekMonday: LocalDate): List<Event> {
        val groupEvents = listOf<GroupEvent>()
        schedule.groupEvents.map { id ->
            if (cachedSchedule.groupEvents.contains(id)) {
                getGroupEvent(id).thenApply { groupEvent ->
                    groupEvents.plus(groupEvent)
                }
            }

        }

        // Transform of groupEvents to a list of Events
        return EventOps.groupEventsToEvents(groupEvents)
    }

    // Note: checks if it is time to prefetch
    fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        currentShownMonday = currentWeekMonday

        return getCurrentEmail().thenCompose { email ->
            if (cachedSchedule.events.isEmpty() && cachedSchedule.groupEvents.isEmpty()) {  // If no cached schedule for that account, we fetch the schedule from the db
                wrappedDatabase.getSchedule(email, currentWeekMonday).thenApply { schedule ->
                    val events = schedule.events.filter {   // We only cache the events that are in the current week or close to it
                        if (it != Event()) {
                            val start = LocalDateTime.parse(it.start).toLocalDate()
                            val end = LocalDateTime.parse(it.end).toLocalDate()
                            start >= minCachedMonday && end <= maxCachedMonday
                        } else {
                            false
                        }
                    }

                    val transformedGroupEvents = fetchGroupEvents(schedule, currentWeekMonday)

                    schedule.copy(events = events + transformedGroupEvents).also {   // Update the cache
                        cachedSchedule = it
                        storeLocalData()
                    }
                }
            }
            else {
                // If it is time to prefetch (because displayed week is too close to the edge of the cached schedule), we fetch the schedule from the db
                if (currentWeekMonday <= minCachedMonday || currentWeekMonday >= maxCachedMonday) {

                    // Update the cached schedule's prefetch boundaries
                    minCachedMonday = currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
                    maxCachedMonday = currentWeekMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

                    wrappedDatabase.getSchedule(email, currentWeekMonday).thenApply { schedule ->
                        val events = schedule.events.filter {
                            val start = LocalDateTime.parse(it.start).toLocalDate()
                            val end = LocalDateTime.parse(it.end).toLocalDate()
                            start >= minCachedMonday && end <= maxCachedMonday
                        }

                        // Transform of groupEvents to a list of Events
                        val transformedGroupEvents = fetchGroupEvents(schedule, currentWeekMonday)

                        schedule.copy(events = events + transformedGroupEvents).also {
                            cachedSchedule = it  // Update the cache
                            storeLocalData()
                        }
                    }
                }
                else {
                    // If no need to prefetch, we return the cached schedule
                    currentShownMonday = currentWeekMonday
                    CompletableFuture.completedFuture(cachedSchedule)
                }
            }
        }
    }

    fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent> {
        return wrappedDatabase.getGroupEvent(groupEventId)
    }

    fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        if (contacts.containsKey(email)) {
            return CompletableFuture.completedFuture(contacts[email])
        }
        return wrappedDatabase.getChatContacts(email).thenApply { it.also { contacts[email] = it } }
    }

    fun getChat(chatId: String): CompletableFuture<Chat> {
        if (chats.containsKey(chatId)) {
            return CompletableFuture.completedFuture(chats[chatId]!!)
        }
        return wrappedDatabase.getChat(chatId).thenApply { it.also { chats[chatId] = it } }
    }

    fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = chats[chatId]!!.copy(messages = chats[chatId]!!.messages + message)
        }
        return wrappedDatabase.sendMessage(chatId, message) // we only the chat with the new message if the chat is already cached
    }

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

    fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        val cachingOnChange = { chat: Chat ->
            chats[chatId] = chat
            onChange(chat)
        }
        wrappedDatabase.addChatListener(chatId, cachingOnChange)
    }

    fun removeChatListener(chatId: String) {
        wrappedDatabase.removeChatListener(chatId)
    }

    // No cache here, method just used for testing to fetch from database
    fun getFCMToken(email: String): CompletableFuture<String> {
        return wrappedDatabase.getFCMToken(email)
    }

    fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        return wrappedDatabase.setFCMToken(email, token)
    }
    fun getCurrentEmail(): CompletableFuture<String> {
        return retrieveData.thenApply {
            if (currentEmail.isNullOrEmpty()) {
                throw IllegalStateException("Current email is null or empty")
            }
            currentEmail
        }
    }

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
        cachedSchedule = Schedule()
        contacts.clear()
        chats.clear()
    }

    /**
     * Check if the device is connected to the internet
     * @param context The context of the application
     * @return True if the device is connected to the internet, false otherwise
     */
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOnline(context: Context): Boolean {
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