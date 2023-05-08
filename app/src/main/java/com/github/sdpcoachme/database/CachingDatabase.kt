package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A caching database that wraps another database
 */
class CachingDatabase(private val wrappedDatabase: Database) : Database {
    private val CACHED_SCHEDULE_WEEKS_AHEAD = 4L
    private val CACHED_SCHEDULE_WEEKS_BEHIND = 4L
    private val cachedUsers = mutableMapOf<String, UserInfo>()
    private val cachedTokens = mutableMapOf<String, String>()
    private val contacts = mutableMapOf<String, List<UserInfo>>()
    private val chats = mutableMapOf<String, Chat>()

    private val cachedSchedules = mutableMapOf<String, List<Event>>()
    private val registeredGroupEvents = mutableListOf<String>()
    private var currentShownMonday = getStartMonday()
    private var minCachedMonday = currentShownMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
    private var maxCachedMonday = currentShownMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

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
    override fun addEvent(event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val email = wrappedDatabase.getCurrentEmail()

        return wrappedDatabase.addEvent(event, currentWeekMonday).thenApply {
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

    override fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void> {
        return wrappedDatabase.addGroupEvent(groupEvent, currentWeekMonday)
    }

    override fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
        return wrappedDatabase.registerForGroupEvent(groupEventId)
        // TODO: add the following line once it is clear how to handle the cache and deletion of group events
            //.thenAccept { registeredGroupEvents.add(groupEventId) }
    }

    // Note: checks if it is time to prefetch
    override fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val email = wrappedDatabase.getCurrentEmail()
        currentShownMonday = currentWeekMonday

        if (!cachedSchedules.containsKey(email)) {  // If no cached schedule for that account, we fetch the schedule from the db
            return wrappedDatabase.getSchedule(currentWeekMonday).thenApply { schedule ->
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

                return wrappedDatabase.getSchedule(currentWeekMonday).thenApply { schedule ->
                    val events = schedule.events.filter {
                        val start = LocalDateTime.parse(it.start).toLocalDate()
                        val end = LocalDateTime.parse(it.end).toLocalDate()
                        start >= minCachedMonday && end <= maxCachedMonday
                    }
                    val groupEvents = listOf<GroupEvent>()
                    schedule.groupEvents.map { id ->
                        getGroupEvent(id, currentWeekMonday).thenApply { groupEvent ->
                            groupEvents.plus(groupEvent)
                        }
                    }

                    // Transform of groupEvents to a list of Events
                    val transformed = EventOps.groupEventsToEvents(groupEvents)

                    schedule.copy(events = events + transformed).also {
                        cachedSchedules[email] = it.events  // Update the cache
                    }
                }
            }
            else {
                // If no need to prefetch, we return the cached schedule
                currentShownMonday = currentWeekMonday
                return CompletableFuture.completedFuture(Schedule(cachedSchedules[email] ?: listOf()))
            }
        }
    }

    override fun getGroupEvent(groupEventId: String, currentWeekMonday: LocalDate): CompletableFuture<GroupEvent> {


        if (registeredGroupEvents.contains(groupEventId)) {
            return wrappedDatabase.getGroupEvent(groupEventId, currentWeekMonday)
        }
        return wrappedDatabase.getGroupEvent(groupEventId, currentWeekMonday)
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

    override fun getFCMToken(email: String): CompletableFuture<String> {
        if (cachedTokens.containsKey(email)) {
            return CompletableFuture.completedFuture(cachedTokens[email])
        }
        return wrappedDatabase.getFCMToken(email).thenApply {
            it.also { cachedTokens[email] = it }
        }
    }

    override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        cachedTokens[email] = token
        return wrappedDatabase.setFCMToken(email, token)
    }

    override fun getCurrentEmail(): String {
        return wrappedDatabase.getCurrentEmail()
    }

    override fun setCurrentEmail(email: String) {
        wrappedDatabase.setCurrentEmail(email)
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
        cachedTokens.clear()
    }
}