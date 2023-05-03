package com.github.sdpcoachme.database

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A caching database that wraps another database
 */
class CachingStore(private val wrappedDatabase: Database,
                   private val datastore: DataStore<Preferences>,
                   context: Context) : Database {


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

    private var retrieveData = if (isInternetAvailable(context)) {
            retrieveLocalData().thenCompose {
                retrieveRemoteData()
            }
        } else retrieveLocalData()

    fun retrieveLocalData(): CompletableFuture<Void> {
        val localFuture = CompletableFuture<Void>()
        GlobalScope.launch {
            val values = datastore.data.first()
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

    private fun <T> processRetrievedCache(jsonString: String?, cache: MutableMap<String, T>) {
        if (jsonString.isNullOrEmpty()) {
            return
        }
        val gson = Gson()
        val type = object : TypeToken<Map<String, T>>() {}.type
        cache.putAll(gson.fromJson(jsonString, type))
    }

    fun retrieveRemoteData(): CompletableFuture<Void> {
        val remoteFuture = CompletableFuture.allOf(
            getAllUsers(),
            getChatContacts(currentEmail!!)
            // todo update other caches
        )
        return remoteFuture
    }

    fun storeLocalData(): CompletableFuture<Void> {
        val writeDatastoreFuture = CompletableFuture<Void>()
        GlobalScope.launch {
            datastore.edit { preferences ->
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

                writeDatastoreFuture.complete(null)
            }
        }
        return writeDatastoreFuture
    }



    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        return wrappedDatabase.updateUser(user).thenAccept { cachedUsers[user.email] = user }
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(cachedUsers[email])
        }
        return wrappedDatabase.getUser(email).thenApply {
            it.also { cachedUsers[email] = it }
        }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return wrappedDatabase.getAllUsers().thenApply {
            it.also {
                cachedUsers.clear()
                cachedUsers.putAll(it.associateBy { it.email }) }
        }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        if (isCached(email)) {
            return CompletableFuture.completedFuture(true)
        }
        return wrappedDatabase.userExists(email)
    }

    // Note: to efficiently use caching, we do not use the wrappedDatabase's addEventsToUser method
    override fun addEvents(events: List<Event>, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        return getCurrentEmail().thenCompose { email ->
            wrappedDatabase.addEvents(events, currentWeekMonday).thenApply {
                // Update the cached schedule
                cachedSchedules[email] = cachedSchedules[email]?.plus(events) ?: events.filter {
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
    override fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val futureEmail = wrappedDatabase.getCurrentEmail()
        currentShownMonday = currentWeekMonday

        return futureEmail.thenCompose { email ->
            if (!cachedSchedules.containsKey(email)) {  // If no cached schedule for that account, we fetch the schedule from the db
                wrappedDatabase.getSchedule(currentWeekMonday).thenApply { schedule ->
                    val events = schedule.events.filter {   // We only cache the events that are in the current week or close to it
                        val start = LocalDateTime.parse(it.start).toLocalDate()
                        val end = LocalDateTime.parse(it.end).toLocalDate()
                        start >= minCachedMonday && end <= maxCachedMonday
                    }
                    schedule.copy(events = events).also {   // Update the cache
                        cachedSchedules[email] = it.events
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

                    wrappedDatabase.getSchedule(currentWeekMonday).thenApply { schedule ->
                        val events = schedule.events.filter {
                            val start = LocalDateTime.parse(it.start).toLocalDate()
                            val end = LocalDateTime.parse(it.end).toLocalDate()
                            start >= minCachedMonday && end <= maxCachedMonday
                        }
                        schedule.copy(events = events).also {
                            cachedSchedules[email] = it.events  // Update the cache
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

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        if (contacts.containsKey(email)) {
            return CompletableFuture.completedFuture(contacts[email])
        }
        return wrappedDatabase.getChatContacts(email).thenApply { it.also { contacts[email] = it } }
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        if (chats.containsKey(chatId)) {
            return CompletableFuture.completedFuture(chats[chatId]!!)
        }
        return wrappedDatabase.getChat(chatId).thenApply { it.also { chats[chatId] = it } }
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = chats[chatId]!!.copy(messages = chats[chatId]!!.messages + message)
        }
        return wrappedDatabase.sendMessage(chatId, message) // we only the chat with the new message if the chat is already cached
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        // Also here, if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = Chat.markOtherUsersMessagesAsRead(
                    chats[chatId]!!,
                    email
                )
        }
        return wrappedDatabase.markMessagesAsRead(chatId, email)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        val cachingOnChange = { chat: Chat ->
            chats[chatId] = chat
            onChange(chat)
        }
        wrappedDatabase.addChatListener(chatId, cachingOnChange)
    }

    override fun removeChatListener(chatId: String) {
        wrappedDatabase.removeChatListener(chatId)
    }

    // No cache here, method just used for testing to fetch from database
    override fun getFCMToken(email: String): CompletableFuture<String> {
        return wrappedDatabase.getFCMToken(email)
    }

    override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        return wrappedDatabase.setFCMToken(email, token)
    }
    override fun getCurrentEmail(): CompletableFuture<String> {
        return retrieveData.thenApply {
            if (currentEmail.isNullOrEmpty()) {
                throw IllegalStateException("Current email is null or empty")
            }
            currentEmail
        }
    }

    override fun setCurrentEmail(email: String): CompletableFuture<Void> {
        currentEmail = email
        // todo change listeners etc here
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


    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}