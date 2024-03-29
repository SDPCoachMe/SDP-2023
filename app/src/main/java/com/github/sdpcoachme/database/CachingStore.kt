package com.github.sdpcoachme.database

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import com.github.sdpcoachme.weather.WeatherForecast
import com.github.sdpcoachme.weather.WeatherPresenter
import com.github.sdpcoachme.weather.api.RetrofitClient
import com.github.sdpcoachme.weather.repository.OpenMeteoRepository
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.math.roundToInt

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
    val CACHED_SCHEDULE_KEY = stringPreferencesKey("cached_schedules")
    val WEATHER_FORECAST = stringPreferencesKey("weather_forecast")

    private val CACHED_SCHEDULE_WEEKS_AHEAD = 4L
    private val CACHED_SCHEDULE_WEEKS_BEHIND = 4L

    // Database-wide stored values
    private val cachedUsers = mutableMapOf<String, UserInfo>()
    private val contacts = mutableMapOf<String, List<UserInfo>>()
    private val contactRowInfos = mutableMapOf<String, List<ContactRowInfo>>()
    private val chats = mutableMapOf<String, Chat>()
    private val cachedTokens = mutableMapOf<String, String>()

    private var cachedSchedule = Schedule()
    private var currentShownMonday = getStartMonday()
    private var minCachedMonday = currentShownMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
    private var maxCachedMonday = currentShownMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

    // Application-wide stored values
    private var currentEmail: String? = null
    private var weatherForecast: WeatherForecast = WeatherForecast()

    private val gson = Gson()

    var retrieveData: CompletableFuture<Void> =
        if (isOnline()) {
            retrieveLocalData().thenAccept { clearCache() }
        } else {
            retrieveLocalData()
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
     *
     * @return a completable future that completes when the local data has been retrieved
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun retrieveLocalData(): CompletableFuture<Void> {
        val localFuture = CompletableFuture<Void>()
        GlobalScope.launch {

            val values = dataStore.data.first()
            currentEmail = values[USER_EMAIL_KEY]

            // Retrieve the Json strings from the datastore
            val serializedUsers = values[CACHED_USERS_KEY]
            val serializedContacts = values[CONTACTS_KEY]
            val serializedChats = values[CHATS_KEY]
            val serializedSchedule = values[CACHED_SCHEDULE_KEY]
            val serializedWeather = values[WEATHER_FORECAST]

            // Deserialize the caching maps from Json and put them in the caching maps
            processRetrievedCache(serializedUsers, cachedUsers)
            processRetrievedCache(serializedContacts, contacts)
            processRetrievedCache(serializedChats, chats)
            processRetrievedCache(serializedSchedule, cachedSchedule)
            processRetrievedCache(serializedWeather, weatherForecast)

            localFuture.complete(null)
        }
        return localFuture
    }

    /**
     * Processes a retrieved cache from Json
     *
     * @param jsonString the Json string to process
     * @param cache the cache to put the processed Json into
     */
    private inline fun <reified T> processRetrievedCache(jsonString: String?, cache: T) {
        if (jsonString.isNullOrEmpty()) {
            return
        }
        val type = object : TypeToken<T>() {}.type
        when (cache) {
            is Schedule -> {
                val schedule = gson.fromJson<Schedule>(jsonString, type)
                cachedSchedule = schedule
            }
            is WeatherForecast -> {
                val forecast = gson.fromJson<WeatherForecast>(jsonString, type)
                weatherForecast = forecast
            }
            else -> {
                cache as MutableMap<*, *>
                cache.putAll(gson.fromJson(jsonString, type))
            }
        }
    }

    /**
     * Stores the given cache in the Datatstore
     * The cache is serialized to Json using the Gson library before being stored
     *
     * @param cache the cache to store
     * @param key the key to store the cache under
     * @param <T> the type of the cache
     * @return a completable future that completes when the cache has been stored
     */
    @OptIn(DelicateCoroutinesApi::class)
    private inline fun <reified T> storeCache(cache: T, key: Preferences.Key<String>) {
        val type = object : TypeToken<T>() {}.type
        // Serialize the cache map to Json
        val serializedCache = gson.toJson(cache, type)
        // Write to datastore in a background coroutine
        GlobalScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = serializedCache
            }
        }
    }

    /**
     * Stores the local data in the datastore
     *
     * @return a completable future that completes when the background write has been launched
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun storeLocalData(): CompletableFuture<Void> {
        val writeDatastoreFuture = CompletableFuture<Void>()
        GlobalScope.launch {
            dataStore.edit { preferences ->
                preferences[USER_EMAIL_KEY] = currentEmail ?: ""
                storeCache(cachedUsers, CACHED_USERS_KEY)
                storeCache(contacts, CONTACTS_KEY)
                storeCache(chats, CHATS_KEY)
                storeCache(cachedSchedule, CACHED_SCHEDULE_KEY)
                storeCache(weatherForecast, WEATHER_FORECAST)

                writeDatastoreFuture.complete(null)
            }
        }
        return writeDatastoreFuture
    }

    /**
     * Updates the current user
     *
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
     *
     * @param email the email of the user to get
     * @return a completable future that completes when the user has been retrieved
     */
    fun getUser(email: String): CompletableFuture<UserInfo> {
        if (isCached(email)) {
            return completedFuture(cachedUsers[email])
        }
        return wrappedDatabase.getUser(email).thenCompose {user ->
            cachedUsers[email] = user
            storeLocalData().thenApply { user }
        }
    }

    /**
     * Gets all users
     *
     * @return a completable future that completes when all users have been retrieved
     */
    fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return if (isOnline()) {
            wrappedDatabase.getAllUsers().thenCompose { userList ->
                cachedUsers.clear()
                cachedUsers.putAll(userList.associateBy { it.email })
                storeLocalData().thenApply { userList }
            }
        } else {
            completedFuture(cachedUsers.values.toList())
        }
    }

    // todo refactor with userLocation inside CachingStore
    /**
     * Get all users from the database sorted by distance from a given location
     *
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
     * Add rating to a coach
     * @param coachEmail The email of the coach
     * @param rating The rating to add
     * @return A future that will complete with the updated user
     */
    fun addRatingToCoach(coachEmail: String, rating: Int): CompletableFuture<Void> {
        return getCurrentEmail().thenCompose { currEmail ->
            getUser(coachEmail).thenCompose { user ->
                if (!user.coach)
                    throw IllegalArgumentException("Adding rating to a non-coach user")
                else if (rating !in 0..5)
                    throw IllegalArgumentException("Rating must be between 0 and 5")
                else {
                    // This replace should be done in the database but was written as a quickfix here
                    val email = currEmail.replace(".", ",")
                    updateUser(user.copy(ratings = user.ratings + (email to rating)))
                }

            }
        }
    }

    /**
     * Get the average rating of a coach rounded to the nearest integer
     * @param email The email of the coach
     * @return A future that will complete with the rating of the coach or null if the user is not a coach
     */
    fun getCoachAverageRating(email: String): CompletableFuture<Int> {
        return getUser(email).thenApply { user ->
            if (user.coach) {
                val ratings = user.ratings.values
                // If the coach has no ratings, return 0 instead of NaN returned by average()
                if (ratings.isEmpty()) 0
                else ratings.average().roundToInt()
            } else
                throw IllegalArgumentException("Getting average rating from a non-coach user")
        }
    }

    fun userExists(email: String): CompletableFuture<Boolean> {
        if (isCached(email)) {
            return completedFuture(true)
        }
        return wrappedDatabase.userExists(email)
    }

    /**
     * Gets the group event with the given id
     */
    fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent> {
        return wrappedDatabase.getGroupEvent(groupEventId)
    }

    /**
     * Gets all group events
     */
    fun getAllGroupEvents(): CompletableFuture<List<GroupEvent>> {
        return wrappedDatabase.getAllGroupEvents()
    }

    /**
     * Gets all group events sorted by date
     */
    fun getAllGroupEventsByDate(): CompletableFuture<List<GroupEvent>> {
        return getAllGroupEvents().thenApply { groupEvents ->
            groupEvents.sortedBy { groupEvent ->
                LocalDateTime.parse(groupEvent.event.start)
            }
        }
    }

    /**
     * Gets all group events that have not yet occurred, sorted by date
     */
    fun getUpcomingGroupEventsByDate(): CompletableFuture<List<GroupEvent>> {
        return getAllGroupEventsByDate().thenApply { groupEvents ->
            groupEvents.filter { groupEvent ->
                LocalDateTime.parse(groupEvent.event.start).isAfter(LocalDateTime.now())
            }
        }
    }

    /**
     * Gets all group events from a given user, sorted by date
     */
    fun getGroupEventsOfUserByDate(email: String): CompletableFuture<List<GroupEvent>> {
        return getAllGroupEventsByDate().thenApply { groupEvents ->
            groupEvents.filter { groupEvent ->
                email in groupEvent.participants || email == groupEvent.organizer
            }
        }
    }

    /**
     * Updates the value of a group event in the caching store (adds it if it does not exist)
     *
     * @param groupEvent The group event to update
     * @return A future that will complete when the group event has been updated.
     */
    fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
        return wrappedDatabase.updateGroupEvent(groupEvent)
    }

    /**
     * Registers the current user for a group event. This updates the group event in the database,
     * updates the schedule of the current user, and adds the current user to the group chat.
     *
     * @param groupEventId the id of the group event to register for
     * @return a completable future that completes when the user has been registered for the group
     * event. The future will complete exceptionally if the group event is full.
     */
    fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
        return getGroupEvent(groupEventId).thenApply { groupEvent ->
            // Check that event is not full
            if (groupEvent.participants.size >= groupEvent.maxParticipants) {
                // TODO: should be a custom exception
                throw Exception("Group event is full")
            }
            groupEvent
        }.thenCompose { groupEvent ->
            getCurrentEmail().thenCompose { email ->

                CompletableFuture.allOf(
                    // Update the group event in the database
                    updateGroupEvent(groupEvent.copy(participants = groupEvent.participants + email)),
                    // Update the schedule of the current user
                    addGroupEventToSchedule(groupEventId),
                    // Add the user to the group chat
                    updateChatParticipants(groupEvent.groupEventId, groupEvent.participants + email),
                    // Update the user contacts and cache
                    addChatContactIfNew(email, groupEvent.groupEventId, groupEvent.groupEventId)
                )
            }
        }
    }

    /**
     * Adds event to the current user's schedule
     *
     * @param event the event to add
     * @return a completable future that completes when the events have been added, containing the cached schedule
     */
    fun addEventToSchedule(event: Event): CompletableFuture<Schedule> {
        return getCurrentEmail().thenCompose { email ->
            wrappedDatabase.addEventToSchedule(email, event).thenApply {
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

    /**
     * Adds a group event to the current user's schedule
     *
     * @param groupEventId the id of the group event to add
     * @return a completable future that completes when the group event has been added, containing the cached schedule
     */
    fun addGroupEventToSchedule(groupEventId: String): CompletableFuture<Schedule> {
        return getCurrentEmail().thenCompose { email ->
            wrappedDatabase.addGroupEventToSchedule(email = email, groupEventId = groupEventId)
        }.thenCompose {
            wrappedDatabase.getGroupEvent(groupEventId)
        }.thenApply { groupEvent ->
            // Update the cached schedule
            cachedSchedule = cachedSchedule.copy(
                events = cachedSchedule.events + EventOps.groupEventsToEvents(listOf(groupEvent)),
                groupEvents = cachedSchedule.groupEvents + groupEventId
            )
            cachedSchedule
        }
    }

    private fun fetchGroupEventsAsEvents(schedule: Schedule): List<Event> {
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
    /**
     * Gets the schedule for the current user
     *
     * @param currentWeekMonday the monday of the current week
     * @return a completable future that completes when the schedule has been retrieved (containing the newly cached schedule)
     */
    fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        currentShownMonday = currentWeekMonday

        return getCurrentEmail().thenCompose { email ->
            if (cachedSchedule.events.isEmpty() && cachedSchedule.groupEvents.isEmpty()) {  // If no cached schedule for that account, we fetch the schedule from the db
                wrappedDatabase.getSchedule(email).thenApply { schedule ->
                    val events =
                        schedule.events.filter {   // We only cache the events that are in the current week or close to it
                            if (it != Event()) {
                                val start = LocalDateTime.parse(it.start).toLocalDate()
                                val end = LocalDateTime.parse(it.end).toLocalDate()
                                start >= minCachedMonday && end <= maxCachedMonday
                            } else {
                                false
                            }
                        }

                    val transformedGroupEvents = fetchGroupEventsAsEvents(schedule)

                    schedule.copy(events = events + transformedGroupEvents)
                        .also {   // Update the cache
                            cachedSchedule = it
                            storeLocalData()
                        }
                }
            } else {
                // If it is time to prefetch (because displayed week is too close to the edge of the cached schedule), we fetch the schedule from the db
                if (currentWeekMonday <= minCachedMonday || currentWeekMonday >= maxCachedMonday) {

                    // Update the cached schedule's prefetch boundaries
                    minCachedMonday = currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
                    maxCachedMonday = currentWeekMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

                    wrappedDatabase.getSchedule(email)
                        .thenApply { schedule ->
                            val events = schedule.events.filter {
                                val start = LocalDateTime.parse(it.start).toLocalDate()
                                val end = LocalDateTime.parse(it.end).toLocalDate()
                                start >= minCachedMonday && end <= maxCachedMonday
                            }

                            // Transform of groupEvents to a list of Events
                            val transformedGroupEvents = fetchGroupEventsAsEvents(schedule)

                            schedule.copy(events = events + transformedGroupEvents).also {
                                cachedSchedule = it  // Update the cache
                                storeLocalData()
                            }
                        }
                } else {
                    // If no need to prefetch, we return the cached schedule
                    currentShownMonday = currentWeekMonday
                    completedFuture(cachedSchedule)
                }
            }
        }
    }

    /**
     * Updates the chat contacts of the current user if the given contact is not already in the list
     *
     * @param email The email of the current user
     * @param chatId The potentially new contact to add
     */
    fun addChatContactIfNew(email: String, chatId: String, contact: String): CompletableFuture<Void> {
        return getUser(email).thenAccept() { user ->
            // Add the other user to the current user's chat contacts if not already inside
            if (!user.chatContacts.contains(contact)) {
                updateCachedContactRowInfo(chatId, Message())

                // update the user in the database
                val updatedUser = user.copy(chatContacts = listOf(contact) + user.chatContacts)
                updateUser(updatedUser)
            }
        }
    }

    /**
     * Get the contact row info for the given user
     * This will be used to display the user's contacts in the UI
     * similar to other messaging services such as WhatsApp:
     * The name of the chat / recipient and the last message will be displayed
     *
     * @param email The email of the user whose contacts should be retrieved
     * @return A future that will complete with the contact row info
     */
    fun getContactRowInfo(email: String): CompletableFuture<List<ContactRowInfo>> {
        if (contactRowInfos.containsKey(email)) {
            return completedFuture(contactRowInfos[email])
        }
        return wrappedDatabase.getContactRowInfos(email).thenApply { it.also { contactRowInfos[email] = it } }
    }

    /**
     * Get a chat
     *
     * @param chatId the id of the chat to get
     * @return a completable future that completes when the chat has been retrieved
     */
    fun getChat(chatId: String): CompletableFuture<Chat> {
        if (chats.containsKey(chatId)) {
            return completedFuture(chats[chatId]!!)
        }
        return wrappedDatabase.getChat(chatId).thenApply {
            it.also {
                chats[chatId] = it
                storeLocalData()
            }
        }
    }

    /**
     * Update / create chat with the following participants
     * If the chat already exists, it will be updated with the new participants
     * If the chat does not exist, it will be created with the given participants
     *
     * @param chatId The id of the chat
     * @param participants The participants of the chat
     * @return A future that will complete when the user has been added
     */
    fun updateChatParticipants(chatId: String, participants: List<String>): CompletableFuture<Void> {
        // if not already cached, we don't cache the chat
        if (chats.containsKey(chatId)) {
            chats[chatId] = chats[chatId]!!.copy(participants = participants)
        }
        return wrappedDatabase.updateChatParticipants(chatId, participants)
    }

    /**
     * Send a message
     *
     * @param chatId the id of the chat to send the message to
     * @param message the message to send
     * @return a completable future that completes when the message has been sent
     */
    fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // if not already cached, we don't cache the chat with the new message (as we would have to fetch the whole chat from the db)
        if (chats.containsKey(chatId)) {
            chats[chatId] = chats[chatId]!!.copy(messages = chats[chatId]!!.messages + message)
            storeLocalData()
        }
        return updateCachedContactRowInfo(chatId, message)
            .thenCompose {
                wrappedDatabase.sendMessage(chatId, message)
            }
    }

    private fun updateCachedContactRowInfo(chatId: String, message: Message): CompletableFuture<Void> {
        return getCurrentEmail().thenAccept { currEmail ->
            // update the contact's last message
            if (!contactRowInfos.containsKey(currEmail)) {
                return@thenAccept
            }

            var existingContacts = listOf<ContactRowInfo>()
            // we need to find the contact with the given chatId and update it
            var updatedContact: ContactRowInfo? = null
            for (contact in contactRowInfos[currEmail]!!) {
                // if the contact is the one we are looking for, we update it
                // but only if the last message has changed (if, e.g.,new members entered the chat,
                // we don't want to place the chat at the top of the list)
                if (contact.chatId == chatId) {
                    updatedContact = contact.copy(lastMessage = message)
                } else {
                    existingContacts = existingContacts + contact
                }
            }
            // Iff the contact is found, we can assume that the cache is
            // up-to-date and return without clearing the cache
            if (updatedContact != null) {
                contactRowInfos[currEmail] = listOf(updatedContact) + existingContacts
                return@thenAccept
            }
            // if the contact is not found, we need to fetch the chat from the db
            // and update the cache with the new contact
            addNewContactToCache(chatId, currEmail, existingContacts)
        }
    }

    /**
     * Add a new contact to the cache
     *
     * @param chatId The id of the chat to add
     * @param currEmail The email of the current user
     * @param existingContacts The existing contacts of the current user
     */
    fun addNewContactToCache(
        chatId: String,
        currEmail: String,
        existingContacts: List<ContactRowInfo>
    ) {
        getChat(chatId).thenCompose { chat ->
            val isGroupChat = chatId.startsWith("@@event")
            val chatTitleFuture =
                if (isGroupChat) getGroupEvent(chatId).thenApply { it.event.name }
                else {
                    val participant = chat.participants.first { it != currEmail }
                    getUser(participant).thenApply { user ->
                        user.firstName + " " + user.lastName
                    }
                }

            chatTitleFuture.thenAccept { chatTitle ->
                val lastMessage = chat.messages.lastOrNull() ?: Message()
                val newContact = ContactRowInfo(
                    chatId,
                    chatTitle,
                    lastMessage,
                    isGroupChat
                )

                contactRowInfos[currEmail] = listOf(newContact) + existingContacts
            }
        }
    }

    /**
     * Mark messages as read
     *
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
     *
     * @param chatId the id of the chat to add the listener to
     * @param onChange the function to call when the chat changes
     */
    fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        val cachingOnChange = { chat: Chat ->
            chats[chatId] = chat
            storeLocalData()
            onChange(chat)
        }
        wrappedDatabase.addChatListener(chatId, cachingOnChange)
    }

    /**
     * Remove a chat listener
     *
     * @param chatId the id of the chat to remove the listener from
     */
    fun removeChatListener(chatId: String) {
        wrappedDatabase.removeChatListener(chatId)
    }

    /**
     * If the device is online, creates a WeatherPresenter and launch the weather forecast pipeline.
     * Cached forecast is updated if the request completes normally.
     * If not, simply returns the cached weather forecast.
     *
     * @param target The target location for the weather forecast
     */
    fun getWeatherForecast(target: LatLng): CompletableFuture<MutableState<WeatherForecast>> {

        return if (isOnline()) {
            // The WeatherPresenter will launch a weather request and return a future that if
            // completed normally will update the cache.
            val weatherPresenter = WeatherPresenter().bind(OpenMeteoRepository(RetrofitClient.api))
            weatherPresenter.getWeatherForecast(target.latitude, target.longitude).thenApply {
                weatherForecast = it
                storeLocalData()
            }
            // we return an observable weather forecast state for the view here
            completedFuture(weatherPresenter.observableWeatherForecast)
        } else {
            completedFuture(mutableStateOf(weatherForecast))
        }
    }

    // No cache here, method just used for testing to fetch from database
    /**
     * Get the FCM token for a user
     *
     * @param email The email of the user to get the FCM token for
     * @return A completable future that completes when the FCM token has been retrieved
     */
    fun getFCMToken(email: String): CompletableFuture<String> {
        if (cachedTokens.containsKey(email)) {
            return completedFuture(cachedTokens[email])
        }
        return wrappedDatabase.getFCMToken(email).thenApply {
            it.also {
                cachedTokens[email] = it
                storeLocalData()
            }
        }
    }

    /**
     * Set the FCM token for a user
     *
     * @param email The email of the user to set the FCM token for
     * @param token The FCM token to set
     * @return A completable future that completes when the FCM token has been set
     */
    fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        cachedTokens[email] = token
        storeLocalData()
        return wrappedDatabase.setFCMToken(email, token)
    }

    /**
     * Get the current email
     *
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
     *
     * @param email The email to set as current
     * @return A completable future that completes when the email has been set
     */
    fun setCurrentEmail(email: String): CompletableFuture<Void> {
        currentEmail = email
        return storeLocalData()
    }

    /**
     * Check if a user is cached
     * Useful for testing
     *
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
        cachedTokens.clear()
        contactRowInfos.clear()
        // we don't clear the weather cache here as getWeatherForecast already makes the
        // isOnline check and updates the cache accordingly.
        // todo do the same for the other caches
    }

    /**
     * Check if the device is connected to the internet
     *
     * @return True if the device is connected to the internet, false otherwise
     */
    fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

}