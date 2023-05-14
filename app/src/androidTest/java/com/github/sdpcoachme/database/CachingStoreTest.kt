package com.github.sdpcoachme.database

// This test class is in the androidTest directory instead of Test directory because it uses
// MockDatabase which is in the androidTest directory.
// Otherwise we would have complicated dependencies.

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import com.github.sdpcoachme.data.ChatSample
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserAddressSamples.Companion.NEW_YORK
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.*
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import junit.framework.TestCase.*
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS

val CACHINGSTORE_TEST_PREFERENCES_NAME = "cachingStore_test_preferences"
val Context.dataStoreTest: DataStore<Preferences> by preferencesDataStore(name = CACHINGSTORE_TEST_PREFERENCES_NAME)

class CachingStoreTest {

    // IMPORTANT:
    // Note that here MockDatabase needs to be re-instantiated for each test as we
    // modify its state in the tests.

    private lateinit var wrappedDatabase: Database
    private lateinit var cachingStore: CachingStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.dataStoreTest.edit { it.clear() }
        }
        wrappedDatabase = MockDatabase()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.dataStoreTest.edit { it.clear() }
        }
    }

    @Test
    fun getUserPutsUserInCache() {
        cachingStore.getUser(exampleEmail).get(5, SECONDS)
        assertTrue(cachingStore.isCached(exampleEmail))
    }

    @Test
    fun addUserPutsUserInCache() {
        val retrievedUser = cachingStore.updateUser(willSmithUser)
            .thenCompose { cachingStore.getUser(willSmithUser.email) }
            .get(5, SECONDS)
        assertTrue(cachingStore.isCached(willSmithUser.email))
        assertTrue(cachingStore.userExists(willSmithUser.email).get(1, SECONDS))
        assertEquals(willSmithUser, retrievedUser)
    }

    class ExistsDB: MockDatabase()  {
        var existsCalled = false
        override fun userExists(email: String): CompletableFuture<Boolean> {
            existsCalled = true
            return CompletableFuture.completedFuture(true)
        }
    }

    @Test
    fun userExistsForUncachedUserFetchesFromWrappedDB() {
        val wrappedDatabase = ExistsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        assertFalse(cachingStore.isCached(willSmithUser.email))
        assertTrue(cachingStore.userExists(willSmithUser.email).get(1, SECONDS))
        assertTrue(wrappedDatabase.existsCalled)
    }

    @Test
    fun addUserOverridesPreviousValueInCache() {
        val wrappedDatabase = ExistsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        val updatedUser = cachingStore.getUser(exampleEmail)
            .thenCompose {
                cachingStore.updateUser(defaultUser) }
            .thenCompose { cachingStore.getUser(exampleEmail) }
            .get(5, SECONDS)
        assertTrue(cachingStore.isCached(exampleEmail))
        assertEquals(defaultUser, updatedUser)
    }

    @Test
    fun getAllUsersPutsAllUsersInCache() {
        val wrappedDatabase = ExistsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        val users = listOf(defaultUser, willSmithUser, rogerFedererUser)
        val setUsers = users.map { cachingStore.updateUser(it) }
        val allUsersInDatabase = CompletableFuture.allOf(*setUsers.toTypedArray())
            .thenApply { cachingStore.clearCache() }
            .thenCompose { cachingStore.getAllUsers() }
            .get(5, SECONDS)
        users.forEach { assertTrue(cachingStore.isCached(it.email)) }
        assertTrue(allUsersInDatabase.containsAll(users))
    }

    @Test
    fun setAndGetCurrentEmail() {
        val email = "test@email.com"
        cachingStore.setCurrentEmail(email)
        assertEquals(email, cachingStore.getCurrentEmail().get(1, SECONDS))
    }

    class ScheduleDBSingleEvent: MockDatabase() {
        var timesCalled = 0
        override fun addEvent(email: String, event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
            timesCalled++
            return CompletableFuture.completedFuture(Schedule(listOf(event)))
        }
    }

    @Test
    fun addEventsAddsThemToWrappedDatabase() {
        val wrappedDatabase = ScheduleDBSingleEvent()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail).get(1000, MILLISECONDS)
        cachingStore.getCurrentEmail().get(1000, MILLISECONDS)
        cachingStore.addEvent(eventList[0], currentMonday)
            .thenCompose { cachingStore.addEvent(eventList[1], currentMonday) }
            .thenCompose { cachingStore.addEvent(eventList[2], currentMonday) }
            .thenCompose { cachingStore.addEvent(eventList[3], currentMonday) }
            .thenCompose { cachingStore.addEvent(eventList[4], currentMonday) }
            .thenCompose { cachingStore.addEvent(eventList[5], currentMonday) }
            .get(5, SECONDS)

        assertThat(wrappedDatabase.timesCalled, `is`(6))
    }

//    class ScheduleDB: MockDatabase() {
//        var timesCalled = 0
//        override fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
//            timesCalled++
//            return CompletableFuture.completedFuture(Schedule(eventList))
//        }
//    }

    class ScheduleDB(schedule: Schedule): MockDatabase() {
        var timesCalled = 0
        var storedSchedule = schedule
        override fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
            timesCalled++
            return CompletableFuture.completedFuture(storedSchedule)
        }
    }


    @Test
    fun getScheduleUsesCacheCorrectlyWithInitialCacheDateRange() {
        val testSchedule = Schedule(cachedEvents)
        val wrappedDatabase = ScheduleDB(testSchedule)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail)
        val isCorrect = cachingStore.getSchedule(currentMonday) // this call should cache the schedule
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled, `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                cachingStore.getSchedule(currentMonday) // this call should return the cached schedule
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled, `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getScheduleWithNewCurrentMondayCachesCorrectSchedule() {
        val testSchedule = Schedule(eventList)
        val wrappedDatabase = ScheduleDB(testSchedule)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail)
        val isCorrect = cachingStore.getSchedule(currentMonday)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled, `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                cachingStore.getSchedule(currentMonday.plusWeeks(6))
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled, `is`(2))
                assertThat(it.events, `is`(nonCachedEvents))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    class ContactsDB: MockDatabase() {
        var timesCalled = 0
        override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
            timesCalled++
            return CompletableFuture.completedFuture(userList)
        }
    }

    @Test
    fun getChatContactsCachesContacts() {
        val wrappedDatabase = ContactsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        val isCorrect = cachingStore.getChatContacts(exampleEmail)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled, `is`(1))
                assertThat(it, `is`(userList))

                cachingStore.getChatContacts(exampleEmail)
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled, `is`(1))
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
        class ContactsDBDefaultChat: MockDatabase() {
            override fun getChat(chatId: String): CompletableFuture<Chat> {
                timesCalled++
                return CompletableFuture.completedFuture(defaultChat)
            }
        }

        wrappedDatabase = ContactsDBDefaultChat()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        val isCorrect = cachingStore.getChat(defaultChat.id)
            .thenCompose {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(defaultChat))

                cachingStore.getChat(defaultChat.id)
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
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val isCorrect = cachingStore.getChat(defaultChat.id) // to place chat into the cache
            .thenCompose { chat ->
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                assertThat(chat, `is`(defaultChat))

                cachingStore.sendMessage(defaultChat.id, newMessage)
                    .thenCompose {
                        cachingStore.getChat(defaultChat.id)
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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val isCorrect = cachingStore.sendMessage(defaultChat.id, newMessage)
            .thenCompose {
                cachingStore.getChat(defaultChat.id)
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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val isCorrect = cachingStore.getChat(defaultChat.id) // to place chat into cache
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))

                cachingStore.markMessagesAsRead(defaultChat.id, defaultUser.email)
                    .thenCompose {
                        cachingStore.getChat(defaultChat.id)
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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val isCorrect = cachingStore.markMessagesAsRead(defaultChat.id, defaultUser.email)
            .thenCompose {
                cachingStore.getChat(defaultChat.id)
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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.addChatListener("chatId") { onChange(it) }

        assertThat(receivedChatId, `is`("chatId"))
        assertTrue(onChangeCalled)

        val isCorrect = cachingStore.getChat("chatId").thenApply {
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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.removeChatListener("chatId")

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
        val cachingDatabase = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val noError = cachingStore.setFCMToken(testEmail, token)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(0))

                cachingStore.getFCMToken(testEmail)
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
        val cachingDatabase = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext())

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
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )

        val noError = cachingStore.setFCMToken(testEmail, token)
            .thenCompose {
                assertThat(wrappedDatabase.timesCalled(), `is`(0))

                cachingStore.getFCMToken("otherEmail")
            }.thenApply {
                assertThat(wrappedDatabase.timesCalled(), `is`(1))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(noError)
    }





    private val exampleEmail = "example@email.com"

    companion object {
        const val exampleEmail = "example@email.com"

        val defaultUser = UserInfo(
            "John",
            "Doe",
            exampleEmail,
            "1234567890",
            LAUSANNE,
            false
        )

        val willSmithUser = UserInfo(
            "Will",
            "Smith",
            "oui@non.com",
            "0000000000",
            NEW_YORK,
            false
        )

        val rogerFedererUser = UserInfo(
            "Roger",
            "Federer",
            "roger@federer.com",
            "1111111111",
            LAUSANNE,
            true
        )

        val userList = listOf(defaultUser, willSmithUser, rogerFedererUser)

        val newMessage = Message(
            "New Message!",
            defaultUser.email,
            LocalDateTime.now().toString(),
            ReadState.SENT
        )
    }

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

}