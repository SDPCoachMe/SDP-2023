package com.github.sdpcoachme.database

// This test class is in the androidTest directory instead of Test directory because it uses
// MockDatabase which is in the androidTest directory.
// Otherwise we would have complicated dependencies.

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.ChatSample
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserAddressSamples.Companion.NEW_YORK
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.*
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture

class CachingDatabaseTest {

    // IMPORTANT:
    // Note that here MockDatabase needs to be re-instantiated for each test as we
    // modify its state in the tests.

    @Test
    fun getUserPutsUserInCache() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.getUser(exampleEmail).get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(exampleEmail))
    }

    @Test
    fun addUserPutsUserInCache() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val retrievedUser = cachingDatabase.updateUser(willSmithUser)
            .thenCompose { cachingDatabase.getUser(willSmithUser.email) }
            .get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(willSmithUser.email))
        assertTrue(cachingDatabase.userExists(willSmithUser.email).get(1, SECONDS))
        assertEquals(willSmithUser, retrievedUser)
    }

    @Test
    fun userExistsForUncachedUserFetchesFromWrappedDB() {
        class ExistsDB: MockDatabase()  {
            var existsCalled = false
            override fun userExists(email: String): CompletableFuture<Boolean> {
                existsCalled = true
                return CompletableFuture.completedFuture(true)
            }
        }

        val wrappedDatabase = ExistsDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        assertFalse(cachingDatabase.isCached(willSmithUser.email))
        assertTrue(cachingDatabase.userExists(willSmithUser.email).get(1, SECONDS))
        assertTrue(wrappedDatabase.existsCalled)
    }

    @Test
    fun addUserOverridesPreviousValueInCache() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val updatedUser = cachingDatabase.getUser(exampleEmail)
            .thenCompose {
                cachingDatabase.updateUser(defaultUser) }
            .thenCompose { cachingDatabase.getUser(exampleEmail) }
            .get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(exampleEmail))
        assertEquals(defaultUser, updatedUser)
    }

    @Test
    fun getAllUsersPutsAllUsersInCache() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val users = listOf(defaultUser, willSmithUser, rogerFedererUser)
        val setUsers = users.map { cachingDatabase.updateUser(it) }
        val allUsersInDatabase = CompletableFuture.allOf(*setUsers.toTypedArray())
            .thenApply { cachingDatabase.clearCache() }
            .thenCompose { cachingDatabase.getAllUsers() }
            .get(5, SECONDS)
        users.forEach { assertTrue(cachingDatabase.isCached(it.email)) }
        assertTrue(allUsersInDatabase.containsAll(users))
    }

    @Test
    fun setAndGetCurrentEmail() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val email = "test@email.com"
        cachingDatabase.setCurrentEmail(email)
        assertEquals(email, cachingDatabase.getCurrentEmail())
    }

    @Test
    fun addEventsAddsThemToWrappedDatabase() {
        var timesCalled = 0
        class ScheduleDB: MockDatabase() {
            override fun addEvent(event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
                timesCalled++
                return CompletableFuture.completedFuture(Schedule(listOf(event)))
            }
        }

        val wrappedDatabase = ScheduleDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)
        val isCorrect = cachingDatabase.addEvent(eventList[0], currentMonday)
            .thenCompose { cachingDatabase.addEvent(eventList[1], currentMonday) }
            .thenCompose { cachingDatabase.addEvent(eventList[2], currentMonday) }
            .thenCompose { cachingDatabase.addEvent(eventList[3], currentMonday) }
            .thenCompose { cachingDatabase.addEvent(eventList[4], currentMonday) }
            .thenCompose { cachingDatabase.addEvent(eventList[5], currentMonday) }
            .thenApply {
                assertThat(timesCalled, `is`(6))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun addGroupEventsAddsThemToWrappedDatabase() {
        var timesCalled = 0
        class ScheduleDB: MockDatabase() {
            override fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void> {
                timesCalled++
                return CompletableFuture.completedFuture(null)
            }
        }

        val wrappedDatabase = ScheduleDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)
        val isCorrect = cachingDatabase.addGroupEvent(groupEvents[0], currentMonday)
            .thenCompose { cachingDatabase.addGroupEvent(groupEvents[1], currentMonday) }
            .thenCompose { cachingDatabase.addGroupEvent(groupEvents[2], currentMonday) }
            .thenCompose { cachingDatabase.addGroupEvent(groupEvents[3], currentMonday) }
            .thenCompose { cachingDatabase.addGroupEvent(groupEvents[4], currentMonday) }
            .thenCompose { cachingDatabase.addGroupEvent(groupEvents[5], currentMonday) }
            .thenApply {
                assertThat(timesCalled, `is`(6))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun registerForGroupEventAddsThemToWrappedDatabase() {
        var timesCalled = 0
        class ScheduleDB: MockDatabase() {
            override fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
                timesCalled++
                return CompletableFuture.completedFuture(null)
            }
        }

        val wrappedDatabase = ScheduleDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)
        val isCorrect = cachingDatabase.registerForGroupEvent(groupEvents[0].groupEventId)
            .thenCompose { cachingDatabase.registerForGroupEvent(groupEvents[1].groupEventId) }
            .thenCompose { cachingDatabase.registerForGroupEvent(groupEvents[2].groupEventId) }
            .thenCompose { cachingDatabase.registerForGroupEvent(groupEvents[3].groupEventId) }
            .thenCompose { cachingDatabase.registerForGroupEvent(groupEvents[4].groupEventId) }
            .thenCompose { cachingDatabase.registerForGroupEvent(groupEvents[5].groupEventId) }
            .thenApply {
                assertThat(timesCalled, `is`(6))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun registerForGroupEventCachesEventId() {
        val isCorrect = true

        // TODO: Implement test

        assertTrue(isCorrect)
    }

    private class GetScheduleDB(val eventList: List<Event>, val groupEvents: List<GroupEvent>): MockDatabase() {
        private var timesCalled = 0

        fun getTimesCalled(): Int {
            return timesCalled
        }

        fun getGroupEventIds(): List<String> {
            return groupEvents.map { it.groupEventId }
        }

        override fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
            timesCalled++
            return CompletableFuture.completedFuture(Schedule(eventList, getGroupEventIds()))
        }
    }
    @Test
    fun getScheduleWithCorrectCacheReturnsCachedSchedule() {
        val wrappedDatabase = GetScheduleDB(eventList, groupEvents) //ScheduleDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)

        val isCorrect = cachingDatabase.getSchedule(currentMonday)
            .thenCompose {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                cachingDatabase.getSchedule(currentMonday)
            }.thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                assertThat(it.groupEvents, `is`(wrappedDatabase.getGroupEventIds()))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getScheduleWithEmptyCacheCachesCorrectSchedule() {
        val wrappedDatabase = GetScheduleDB(eventList, groupEvents)
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)

        val isCorrect = cachingDatabase.getSchedule(currentMonday)
            .thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                assertThat(it.groupEvents, `is`(wrappedDatabase.getGroupEventIds()))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getScheduleWithNewCurrentMondayCachesCorrectSchedule() {
        val wrappedDatabase = GetScheduleDB(eventList, groupEvents)
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.setCurrentEmail(exampleEmail)

        val isCorrect = cachingDatabase.getSchedule(currentMonday)
            .thenCompose {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                cachingDatabase.getSchedule(currentMonday.plusWeeks(6))
            }.thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(2))
                assertThat(it.events, `is`(nonCachedEvents))
                assertThat(it.groupEvents, `is`(wrappedDatabase.getGroupEventIds()))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getChatContactsCachesContacts() {
        var timesCalled = 0
        class ContactsDB: MockDatabase() {
            override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
                timesCalled++
                return CompletableFuture.completedFuture(userList)
            }
        }

        val wrappedDatabase = ContactsDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val isCorrect = cachingDatabase.getChatContacts(exampleEmail)
            .thenCompose {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(userList))

                cachingDatabase.getChatContacts(exampleEmail)
            }.thenApply {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(userList))

                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getChatCachesTheChat() {
        var timesCalled = 0
        class ContactsDB: MockDatabase() {
            override fun getChat(chatId: String): CompletableFuture<Chat> {
                timesCalled++
                return CompletableFuture.completedFuture(defaultChat)
            }
        }

        val wrappedDatabase = ContactsDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val isCorrect = cachingDatabase.getChat(defaultChat.id)
            .thenCompose {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(defaultChat))

                cachingDatabase.getChat(defaultChat.id)
            }.thenApply {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(defaultChat))

                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    private class SendMessageDB(val defaultChat: Chat): MockDatabase() {
        var timesCalled = 0

        fun timesCalled() = timesCalled
        override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
            return CompletableFuture.completedFuture(null)
        }

        override fun getChat(chatId: String): CompletableFuture<Chat> {
            timesCalled++
            return CompletableFuture.completedFuture(defaultChat)
        }
    }
    @Test
    fun sendingMessageForCachedChatUpdatesThatChatInsideTheCache() {
        val newMessage = Message(
            "New Message!",
            defaultUser.email,
            LocalDateTime.now().toString(),
            ReadState.SENT
        )
        val expectedChat = defaultChat.copy(messages = defaultMessages + newMessage)

        val wrappedDatabase = SendMessageDB(defaultChat)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val isCorrect = cachingDatabase.getChat(defaultChat.id) // to place chat into the cache
            .thenCompose { chat ->
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                assertThat(chat, `is`(defaultChat))

                cachingDatabase.sendMessage(defaultChat.id, newMessage)
                    .thenCompose {
                        cachingDatabase.getChat(defaultChat.id)
                    }.thenApply {
                        // should not have called the database again as it is cached
                        assertThat(wrappedDatabase.timesCalled(), `is`(1))
                        assertThat(it, `is`(expectedChat))

                        true
                    }.exceptionally {
                        false
                    }
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun sendingMessageForNotCachedChatDoesNotCacheTheChat() {
        val newMessage = Message(
            "New Message!",
            defaultUser.email,
            LocalDateTime.now().toString(),
            ReadState.SENT
        )

        val wrappedDatabase = SendMessageDB(defaultChat)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val isCorrect = cachingDatabase.sendMessage(defaultChat.id, newMessage)
            .thenCompose {
                cachingDatabase.getChat(defaultChat.id)
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                assertThat(it, `is`(defaultChat))

                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    private class MarkMessagesAsReadDB(val defaultChat: Chat): MockDatabase() {
        var timesCalled = 0

        fun timesCalled() = timesCalled
        override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
            return CompletableFuture.completedFuture(null)
        }

        override fun getChat(chatId: String): CompletableFuture<Chat> {
            timesCalled++
            return CompletableFuture.completedFuture(defaultChat)
        }
    }

    @Test
    fun markMessagesAsReadForCachedChatUpdatesCache() {
        val expectedChat = defaultChat.copy(messages = defaultChat.messages.map {
            if (it.sender != defaultUser.email)
                it.copy(readState = ReadState.READ)
            else
                it
        })
        val wrappedDatabase = MarkMessagesAsReadDB(defaultChat)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val isCorrect = cachingDatabase.getChat(defaultChat.id) // to place chat into cache
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))

                cachingDatabase.markMessagesAsRead(defaultChat.id, defaultUser.email)
                    .thenCompose {
                        cachingDatabase.getChat(defaultChat.id)
                            .thenApply {
                                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                                assertThat(it, `is`(expectedChat))

                                true
                            }.exceptionally {
                                false
                            }
                    }
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun markMessageAsReadForNotCachedChatDoesNotCacheTheChat() {
        val wrappedDatabase = MarkMessagesAsReadDB(defaultChat)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val isCorrect = cachingDatabase.markMessagesAsRead(defaultChat.id, defaultUser.email)
            .thenCompose {
                cachingDatabase.getChat(defaultChat.id)
                    .thenApply {
                        assertThat(wrappedDatabase.timesCalled(), `is`(1))
                        assertThat(it, `is`(defaultChat)) // as our SendMessageDB does not update the chat for simplicity

                        true
                    }.exceptionally {
                        false
                    }
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun addChatListenerPropagatesToWrappedDatabase() {
        var timesCalled = 0
        var receivedChatId = ""
        var onChangeCalled = false
        val onChange: (Chat) -> Unit =  { (_) -> run { onChangeCalled = true } }
        class AddChatListenerDB: MockDatabase() {
            override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
                receivedChatId = chatId
                // called to simulate a change in the database and test the caching database
                onChange(defaultChat)
            }

            override fun getChat(chatId: String): CompletableFuture<Chat> {
                timesCalled++
                return CompletableFuture.completedFuture(Chat())
            }
        }

        val wrappedDatabase = AddChatListenerDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.addChatListener("chatId") { onChange(it) }

        assertThat(receivedChatId, `is`("chatId"))
        assertTrue(onChangeCalled)

        val isCorrect = cachingDatabase.getChat("chatId").thenApply {
            assertThat(timesCalled, `is`(0))
            assertThat(it, `is`(defaultChat))
            true
        }.exceptionally { false }
            .get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun removeChatListenerPropagatesToWrappedDatabase() {
        var receivedChatId = ""
        class RemoveChatListenerDB: MockDatabase() {
            override fun removeChatListener(chatId: String) {
                receivedChatId = chatId
            }
        }

        val wrappedDatabase = RemoveChatListenerDB()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        cachingDatabase.removeChatListener("chatId")

        assertThat(receivedChatId, `is`("chatId"))
    }

    private class TokenDB(var token: String, val email: String): MockDatabase() {
        private var timesFetched = 0

        fun timesCalled() = timesFetched

        override fun getFCMToken(email: String): CompletableFuture<String> {
            timesFetched++
            return CompletableFuture.completedFuture(if (email == this.email) token else "")
        }

        override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
            if (email == this.email)
                this.token = token
            return CompletableFuture.completedFuture(null)
        }
    }
    @Test
    fun getFcmTokenCachesTheTokenForSubsequentRequest() {
        val testEmail = "example@email.com"
        val token = "------token-----"

        val wrappedDatabase = TokenDB(token, testEmail)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val noError = cachingDatabase.getFCMToken(testEmail)
            .thenCompose {
                assertThat(it, `is`(token))
                assertThat(wrappedDatabase.timesCalled(), `is`(1))

                cachingDatabase.getFCMToken(testEmail)
            }.thenApply {
                assertThat(it, `is`(token))
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(noError)
    }

    @Test
    fun setFCMTokenCachesItForSubsequentGets() {
        val testEmail = "example@email.com"
        val token = "------token-----"

        val wrappedDatabase = TokenDB(token, testEmail)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val noError = cachingDatabase.setFCMToken(testEmail, token)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(0))

                cachingDatabase.getFCMToken(testEmail)
            }.thenApply {
                assertThat(it, `is`(token))
                assertThat(wrappedDatabase.timesCalled(), `is`(0))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(noError)
    }

    @Test
    fun getFcmTokenOnDifferentEmailsFetchesTwiceFromWrappedDB() {
        val testEmail = "example@email.com"
        val token = "------token-----"

        val wrappedDatabase = TokenDB(token, testEmail)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val noError = cachingDatabase.getFCMToken(testEmail)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))

                cachingDatabase.getFCMToken("otherEmail")
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled(), `is`(2))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(noError)
    }

    @Test
    fun setAndgetFcmTokenOnDifferentEmailsFetchesFromWrappedDB() {
        val testEmail = "example@email.com"
        val token = "------token-----"

        val wrappedDatabase = TokenDB(token, testEmail)
        val cachingDatabase = CachingDatabase(wrappedDatabase)

        val noError = cachingDatabase.setFCMToken(testEmail, token)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(0))

                cachingDatabase.getFCMToken("otherEmail")
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(noError)
    }




    private val exampleEmail = "example@email.com"

    private val defaultUser = UserInfo(
        "John",
        "Doe",
        exampleEmail,
        "1234567890",
        LAUSANNE,
        false
    )

    private val willSmithUser = UserInfo(
        "Will",
        "Smith",
        "oui@non.com",
        "0000000000",
        NEW_YORK,
        false
    )

    private val rogerFedererUser = UserInfo(
        "Roger",
        "Federer",
        "roger@federer.com",
        "1111111111",
        LAUSANNE,
        true
    )

    private val userList = listOf(defaultUser, willSmithUser, rogerFedererUser)
    private val defaultMessages = ChatSample.MESSAGES
    private val defaultChat = Chat().copy(
        id = "defaultId",
        participants = listOf(willSmithUser.email, defaultUser.email),
        messages = defaultMessages
    )


    private val currentMonday: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val cachedEvents = listOf(
        Event(
            name = "Google I/O Keynote",
            color = Color(0xFFAFBBF2).value.toString(),
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0).toString(),
            end = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0).toString(),
            description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
        ),
        Event(
            name = "Developer Keynote",
            color = Color(0xFFAFBBF2).value.toString(),
            start = currentMonday.plusDays(2).atTime(7, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(9, 0, 0).toString(),
            description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
        ),
        Event(
            name = "What's new in Android",
            color = Color(0xFF1B998B).value.toString(),
            start = currentMonday.plusDays(2).atTime(10, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(12, 0, 0).toString(),
            description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        ),
        Event(
            name = "What's new in Machine Learning",
            color = Color(0xFFF4BFDB).value.toString(),
            start = currentMonday.plusDays(2).atTime(22, 0, 0).toString(),
            end = currentMonday.plusDays(3).atTime(4, 0, 0).toString(),
            description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        ),
        Event(
            name = "What's new in Material Design",
            color = Color(0xFF6DD3CE).value.toString(),
            start = currentMonday.plusDays(3).atTime(13, 0, 0).toString(),
            end = currentMonday.plusDays(3).atTime(15, 0, 0).toString(),
            description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
        )
    )

    private val nonCachedEvents = listOf(
        Event(
            name = "Event outside of cache borders",
            color = Color(0xFF6DD3CE).value.toString(),
            start = currentMonday.plusWeeks(5).atTime(13, 0, 0).toString(),
            end = currentMonday.plusWeeks(5).atTime(15, 0, 0).toString(),
        )
    )


    private val eventList = cachedEvents + nonCachedEvents

    private val groupEvents = eventList.map {event ->
        GroupEvent(
            event.copy(name = "${event.name} (group event)", start = event.end, end = LocalDateTime.parse(event.end).plusHours(1).format(EventOps.getEventDateFormatter())),
            MockDatabase.getDefaultEmail(),
            3,
        )
    }

}