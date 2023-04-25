package com.github.sdpcoachme.database

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A caching database that wraps another database
 */
class CachingDatabase(private val wrappedDatabase: Database) : Database {
    private val CACHED_SCHEDULE_WEEKS_AHEAD = 4L
    private val CACHED_SCHEDULE_WEEKS_BEHIND = 1L
    private val cachedUsers = mutableMapOf<String, UserInfo>()

    private val cachedSchedules = mutableMapOf<String, List<Event>>()
    private var currentShownMonday = getStartMonday()
    private val minCachedMonday = currentShownMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND)
    private val maxCachedMonday = currentShownMonday.plusWeeks(CACHED_SCHEDULE_WEEKS_AHEAD)

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

    private fun getCacheKey(email: String, currentWeekMonday: LocalDate): String {
        return "$email:$currentWeekMonday"
    }

    // Note: to efficiently use caching, we do not use the wrappedDatabase's addEventsToUser method
    override fun addEvents(email: String, events: List<Event>, currentWeekMonday: LocalDate): CompletableFuture<Void> {
        /*return getUser(email).thenCompose {
            val updatedUserInfo = it.copy(events = it.events + events)
            updateUser(updatedUserInfo)
        }*/
        // TODO: check this and make some comments
        return wrappedDatabase.addEvents(email, events, currentWeekMonday).thenAccept {
            cachedSchedules[email] = cachedSchedules[email]?.plus(events) ?: events.filter {
                val start = LocalDateTime.parse(it.start).toLocalDate()
                val end = LocalDateTime.parse(it.end).toLocalDate()
                start >= currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND) && end <= currentWeekMonday.plusWeeks(
                    CACHED_SCHEDULE_WEEKS_AHEAD + 1
                )
            }
        }
    }

    // Note: checks if it is time to prefetch
    override fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        return if (currentWeekMonday <= minCachedMonday || currentWeekMonday >= maxCachedMonday) {
            cachedSchedules.remove(email)
            currentShownMonday = currentWeekMonday
            wrappedDatabase.getSchedule(email, currentWeekMonday).thenApply {
                it.copy(events = it.events.filter {
                    val start = LocalDateTime.parse(it.start).toLocalDate()
                    val end = LocalDateTime.parse(it.end).toLocalDate()
                    start >= currentWeekMonday.minusWeeks(CACHED_SCHEDULE_WEEKS_BEHIND) && end <= currentWeekMonday.plusWeeks(
                        CACHED_SCHEDULE_WEEKS_AHEAD + 1
                    )
                })
            }
        } else {
            CompletableFuture.completedFuture(Schedule(email, cachedSchedules[email] ?: listOf()))
        }
    }

    override fun getEvents(email: String, currentWeekMonday: LocalDate): CompletableFuture<List<Event>> {
        return getSchedule(email, currentWeekMonday).thenApply { it.events }
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        // TODO implement in next sprint
        return wrappedDatabase.getChatContacts(email)
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        // TODO implement in next sprint
        return wrappedDatabase.getChat(chatId)
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // TODO implement in next sprint
        return wrappedDatabase.sendMessage(chatId, message)
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        // TODO implement in next sprint
        return wrappedDatabase.markMessagesAsRead(chatId, email)
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        // TODO implement in next sprint (adapt onChange to change this here and then call the passed onChange!)
        wrappedDatabase.addChatListener(chatId, onChange)
    }

    override fun removeChatListener(chatId: String) {
        // TODO implement in next sprint
        wrappedDatabase.removeChatListener(chatId)
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
    }
}