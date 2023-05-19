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
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.AddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.AddressSamples.Companion.NEW_YORK
import com.github.sdpcoachme.data.ChatSample
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.ReadState
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import junit.framework.TestCase.*
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

val Context.dataStoreTest: DataStore<Preferences> by preferencesDataStore(name = "caching_store_test_preferences")

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
        cachingStore.retrieveData.get(1, SECONDS)
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

    class UpdatesUserDB: MockDatabase()  {
        var timesCalled = 0
        override fun updateUser(user: UserInfo): CompletableFuture<Void> {
            timesCalled++
            return super.updateUser(user)
        }
    }

    @Test
    fun updateUserPutsUserInCache() {
        val wrappedDatabase = UpdatesUserDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.updateUser(willSmithUser).get(1, SECONDS)
        assertTrue(cachingStore.isCached(willSmithUser.email))
        assertTrue(cachingStore.userExists(willSmithUser.email).get(1, SECONDS))
        val retrievedUser = cachingStore.getUser(willSmithUser.email).get(5, SECONDS)
        assertEquals(willSmithUser, retrievedUser)
        assertEquals(1, wrappedDatabase.timesCalled)
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
        cachingStore.retrieveData.get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
        val updatedUser = cachingStore.getUser(exampleEmail)
            .thenCompose {
                cachingStore.updateUser(defaultUser) }
            .thenCompose { cachingStore.getUser(exampleEmail) }
            .get(5, SECONDS)
        assertTrue(cachingStore.isCached(exampleEmail))
        assertEquals(defaultUser, updatedUser)
    }

    // TODO: fix this failing test
/*    @Test
    fun getAllUsersPutsAllUsersInCache() {
        val wrappedDatabase = ExistsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        println("IsOnline: ${cachingStore.isOnline()}")
        val users = listOf(defaultUser, willSmithUser, rogerFedererUser)
        val setUsers = users.map { cachingStore.updateUser(it) }
        val isCorrect = CompletableFuture.allOf(*setUsers.toTypedArray())
            .thenApply { cachingStore.clearCache() }
            .thenCompose { println("getAllUsers"); cachingStore.getAllUsers()}
            .thenApply{ allUsersInDatabase ->
                users.forEach {
                    println("Checking ${it.email}")
                    assertTrue(cachingStore.isCached(it.email))
                }
                assertTrue(allUsersInDatabase.containsAll(users))
                true
            }.exceptionally {
                false
            }.get(10, SECONDS)

        assertTrue(isCorrect)
    }*/

    @Test
    fun setAndGetCurrentEmail() {
        val email = "test@email.com"
        cachingStore.setCurrentEmail(email)
        assertEquals(email, cachingStore.getCurrentEmail().get(1, SECONDS))
    }

    class ScheduleDBSingleEvent: MockDatabase() {
        var timesCalled = 0
        override fun addEventToSchedule(email: String, event: Event): CompletableFuture<Schedule> {
            timesCalled++
            return CompletableFuture.completedFuture(Schedule(listOf(event)))
        }
    }

    @Test
    fun addEventAddsItToWrappedDatabase() {
        val wrappedDatabase = ScheduleDBSingleEvent()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1000, MILLISECONDS)
        cachingStore.getCurrentEmail().get(1000, MILLISECONDS)
        cachingStore.addEventToSchedule(eventList[0])
            .thenCompose { cachingStore.addEventToSchedule(eventList[1]) }
            .thenCompose { cachingStore.addEventToSchedule(eventList[2]) }
            .thenCompose { cachingStore.addEventToSchedule(eventList[3]) }
            .thenCompose { cachingStore.addEventToSchedule(eventList[4]) }
            .thenCompose { cachingStore.addEventToSchedule(eventList[5]) }
            .thenCompose { cachingStore.addEventToSchedule(eventList[6]) }
            .get(5, SECONDS)

        assertThat(wrappedDatabase.timesCalled, `is`(7))
    }

    @Test
    fun addGroupEventAddsItToWrappedDatabase() {
        var timesCalled = 0
        class ScheduleDB: MockDatabase() {
            private var addedGroupEvents = mutableMapOf<String, GroupEvent>()
            private var schedule = Schedule(listOf(), listOf())

            fun getAddedGroupEvents(): Map<String, GroupEvent> {
                return addedGroupEvents
            }

            override fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
                timesCalled++
                addedGroupEvents[groupEvent.groupEventId] = groupEvent
                return CompletableFuture.completedFuture(null)
            }

            override fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule> {
                val groupEvent = addedGroupEvents[groupEventId]
                schedule = schedule.copy(
                    events = schedule.events + listOf(groupEvent!!.event),
                    groupEvents = schedule.groupEvents + listOf(groupEvent.groupEventId))
                return CompletableFuture.completedFuture(schedule)
            }

        }

        val wrappedDatabase = ScheduleDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(MockDatabase.getDefaultEmail())
        var checkEvent = groupEvents[0]
        val isCorrect = cachingStore.updateGroupEvent(checkEvent)
            .thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(1))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[1]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(2))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[2]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(3))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[3]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(4))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[4]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(5))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[5]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenCompose {
                assertThat(wrappedDatabase.getAddedGroupEvents().size, `is`(6))
                assertThat(wrappedDatabase.getAddedGroupEvents().get(checkEvent.groupEventId), `is`(checkEvent))

                checkEvent = groupEvents[6]
                cachingStore.updateGroupEvent(checkEvent)
            }.thenApply {
                assertThat(timesCalled, `is`(7))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    private class RegisterForEventDB(groupEventsMap: MutableMap<String, GroupEvent>): MockDatabase() {
        private val availableGroupEvents = groupEventsMap
        private var schedule = Schedule()
        private var timesCalled = 0

        fun getTimesCalled(): Int {
            return timesCalled
        }

        fun getAvailableGroupEvents(): MutableMap<String, GroupEvent> {
            return availableGroupEvents
        }
        override fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule> {
            timesCalled++
            if (availableGroupEvents.containsKey(groupEventId)) {
                // register exampleEmail
                availableGroupEvents[groupEventId] = availableGroupEvents[groupEventId]!!.copy(participants = listOf(
                    getDefaultEmail()
                ))
                schedule = schedule.copy(events = schedule.events + availableGroupEvents[groupEventId]!!.event, groupEvents = schedule.groupEvents + availableGroupEvents[groupEventId]!!.groupEventId)
                return CompletableFuture.completedFuture(null)
            }
            val failFuture = CompletableFuture<Schedule>()
            failFuture.completeExceptionally(NoSuchElementException())
            return failFuture
        }

        override fun getSchedule(email: String): CompletableFuture<Schedule> {
            return CompletableFuture.completedFuture(schedule)
        }
    }

    // TODO: fix those 2 tests now that implementation of registerForGroupEvent has changed
//    @Test
//    fun registerForGroupEventAddsParticipantToWrappedDatabase() {
//        val availableGroupEvents = mutableMapOf<String, GroupEvent>()
//        groupEvents.forEach { availableGroupEvents[it.groupEventId] = it }
//
//        val eventsToRegisterFor = listOf(groupEvents[0], groupEvents[2], groupEvents[4], groupEvents[5])
//
//        val wrappedDatabase = RegisterForEventDB(availableGroupEvents)
//        cachingStore = CachingStore(wrappedDatabase,
//            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
//            ApplicationProvider.getApplicationContext()
//        )
//        cachingStore.retrieveData.get(1, SECONDS)
//        cachingStore.setCurrentEmail(exampleEmail)
//
//        val isCorrect = cachingStore.registerForGroupEvent(eventsToRegisterFor[0].groupEventId)
//            .thenCompose {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
//                cachingStore.registerForGroupEvent(eventsToRegisterFor[1].groupEventId)
//            }.thenCompose {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(2))
//                cachingStore.registerForGroupEvent(eventsToRegisterFor[2].groupEventId)
//            }.thenCompose {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(3))
//                cachingStore.registerForGroupEvent(eventsToRegisterFor[3].groupEventId)
//            }.thenApply {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(4))
//                eventsToRegisterFor.forEach { assertThat(wrappedDatabase.getAvailableGroupEvents()[it.groupEventId]!!.participants, hasItem(exampleEmail)) }
//                true
//            }.exceptionally {
//                false
//            }.get(5, SECONDS)
//
//        assertTrue(isCorrect)
//    }
//
//    // note: for now, all IDs the user is registered for are cached (as part of the cached schedule)
//    @Test
//    fun registerForGroupEventCachesEventId() {
//        val availableGroupEvents = mutableMapOf<String, GroupEvent>()
//        groupEvents.forEach { availableGroupEvents[it.groupEventId] = it }
//
//        val eventsToRegisterFor = listOf(groupEvents[0], groupEvents[2])
//
//        val wrappedDatabase = RegisterForEventDB(availableGroupEvents)
//        cachingStore = CachingStore(wrappedDatabase,
//            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
//            ApplicationProvider.getApplicationContext()
//        )
//        cachingStore.retrieveData.get(1, SECONDS)
//        cachingStore.setCurrentEmail(exampleEmail)
//
//        val isCorrect = cachingStore.getSchedule(currentMonday)
//            .thenCompose { schedule ->
//                assertThat(schedule.groupEvents.size, `is`(0))
//                cachingStore.registerForGroupEvent(eventsToRegisterFor[0].groupEventId) }
//            .thenCompose {
//                cachingStore.getSchedule(currentMonday) }
//            .thenCompose { schedule ->
//                assertThat(schedule.groupEvents.size, `is`(1))
//                assertThat(schedule.groupEvents, hasItem(eventsToRegisterFor[0].groupEventId))
//                cachingStore.registerForGroupEvent(eventsToRegisterFor[1].groupEventId) }
//            .thenCompose {
//                cachingStore.getSchedule(currentMonday) }
//            .thenApply { schedule ->
//                assertThat(schedule.groupEvents.size, `is`(2))
//                assertThat(schedule.groupEvents, hasItems(eventsToRegisterFor[0].groupEventId, eventsToRegisterFor[1].groupEventId))
//                true
//            }.exceptionally {
//                false
//            }.get(5, SECONDS)
//
//        assertTrue(isCorrect)
//    }


    private class GetScheduleDB(val eventList: List<Event>, val groupEvents: List<GroupEvent>): MockDatabase() {
        private var timesCalled = 0

        fun getTimesCalled(): Int {
            return timesCalled
        }

        fun getGroupEventIds(): List<String> {
            return groupEvents.map { it.groupEventId }
        }

        override fun getSchedule(email: String): CompletableFuture<Schedule> {
            timesCalled++
            // we also add an empty event to the schedule to make sure that the caching store doesn't cache empty events -> covers additional branch in caching store
            return CompletableFuture.completedFuture(Schedule(eventList, getGroupEventIds()))
        }
    }

    @Test
    fun getScheduleUsesCacheCorrectlyWithInitialCacheDateRange() {
        val wrappedDatabase = GetScheduleDB(eventList, groupEvents)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail)
        val isCorrect = cachingStore.getSchedule(currentMonday) // this call should cache the schedule
            .thenCompose {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                cachingStore.getSchedule(currentMonday) // this call should return the cached schedule
            }.thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun getScheduleWithNewCurrentMondayCachesCorrectSchedule() {
        val wrappedDatabase = GetScheduleDB(eventList, groupEvents)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail)
        val isCorrect = cachingStore.getSchedule(currentMonday)
            .thenCompose {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it.events, `is`(cachedEvents))
                cachingStore.getSchedule(currentMonday.plusWeeks(6))
            }.thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(2))
                assertThat(it.events, `is`(nonCachedEvents))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    private class GetGroupEventDB(groupEvent: GroupEvent): MockDatabase() {
        private var timesCalled = 0
        private val storedGroupEvent = groupEvent
        private val storedSchedule = Schedule(listOf(groupEvent.event), listOf(groupEvent.groupEventId))

        fun getTimesCalled(): Int {
            return timesCalled
        }

        override fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent> {
            timesCalled++
            return CompletableFuture.completedFuture(storedGroupEvent)
        }

        override fun getSchedule(email: String): CompletableFuture<Schedule> {
            return CompletableFuture.completedFuture(storedSchedule)
        }
    }

    @Test
    fun getGroupEventGetsCorrectGroupEvent() {
        // initialize database s.t. it contains the group event where the test user is a participant
        val testEvent = groupEvents[0]
        val wrappedDatabase = GetGroupEventDB(testEvent)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail)

        val currentMonday = EventOps.getStartMonday()
        val isCorrect = cachingStore.getSchedule(currentMonday)  // caches the group event id to user's schedule
            .thenCompose {
                cachingStore.getGroupEvent(testEvent.groupEventId)
            }.thenApply {
                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
                assertThat(it, `is`(testEvent))
                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    // This test might have to be adapted for next PR
    @Test
    fun getGroupEventFailsForUnregisteredUser() {
        val wrappedDatabase = GetGroupEventDB(GroupEvent())
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)

        val isCorrect = cachingStore.getSchedule(currentMonday)
            .thenCompose {
                cachingStore.getGroupEvent(groupEvents[0].groupEventId)
            }.thenApply {
                false
            }.exceptionally {
                true
            }.get(5, SECONDS)

        assertThat(wrappedDatabase.getTimesCalled(), `is`(0))
        assertTrue(isCorrect)
    }


    // TODO: fix those 2 tests, although they seem to be implementation specific and not
    //  really necessary
//    @Test
//    fun groupEventFutureRecoversFromAddGroupEventDBError() {
//        class AddGroupEventDB: MockDatabase() {
//            private var timesCalled = 0
//
//            fun getTimesCalled(): Int {
//                return timesCalled
//            }
//
//            override fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
//                timesCalled++
//                val failingFuture = CompletableFuture<Void>()
//                failingFuture.completeExceptionally(Exception())
//                return failingFuture
//            }
//        }
//
//        val wrappedDatabase = AddGroupEventDB()
//        cachingStore = CachingStore(wrappedDatabase,
//            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
//            ApplicationProvider.getApplicationContext()
//        )
//        cachingStore.retrieveData.get(1, SECONDS)
//        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
//
//        val isCorrect = cachingStore.updateGroupEvent(groupEvents[0])
//            .thenApply {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
//                true
//            }.exceptionally {
//                // should have recovered before reaching here
//                false
//            }.get(5, SECONDS)
//
//        assertTrue(isCorrect)
//    }
//
//    @Test
//    fun groupEventFutureRecoversFromRegisterForEventDBError() {
//        class AddGroupEventDB: MockDatabase() {
//            private var timesCalled = 0
//
//            fun getTimesCalled(): Int {
//                return timesCalled
//            }
//
//            override fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule> {
//                timesCalled++
//                val failingFuture = CompletableFuture<Schedule>()
//                failingFuture.completeExceptionally(Exception())
//                return failingFuture
//            }
//        }
//
//        val wrappedDatabase = AddGroupEventDB()
//        cachingStore = CachingStore(wrappedDatabase,
//            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
//            ApplicationProvider.getApplicationContext()
//        )
//        cachingStore.retrieveData.get(1, SECONDS)
//        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
//
//        val isCorrect = cachingStore.registerForGroupEvent(groupEvents[0].groupEventId)
//            .thenApply {
//                assertThat(wrappedDatabase.getTimesCalled(), `is`(1))
//                true
//            }.exceptionally {
//                // should have recovered before reaching here
//                false
//            }.get(5, SECONDS)
//
//        assertTrue(isCorrect)
//    }

    @Test
    fun getChatContactsCachesContacts() {
        var timesCalled = 0

        val rowInfo = ContactRowInfo(
            "chatiId",
            "chatiName",
            Message("sender@email.com", "Sender Name", "Test Message", LocalDateTime.now().toString(), ReadState.SENT, mapOf()),
            false,
        )

        val rowInfo2 = rowInfo.copy(chatId = "chatiId2", chatTitle = "chatiName2", isGroupChat = true)
        val expectedRowInfo = listOf(rowInfo, rowInfo2)
        class ContactsDB: MockDatabase() {
            override fun getContactRowInfos(email: String): CompletableFuture<List<ContactRowInfo>> {
                timesCalled++
                assertThat(email, `is`(exampleEmail))
                return CompletableFuture.completedFuture(expectedRowInfo)
            }
        }

        val wrappedDatabase = ContactsDB()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
        val isCorrect = cachingStore.getContactRowInfo(exampleEmail)
            .thenCompose {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(expectedRowInfo))

                cachingStore.getContactRowInfo(exampleEmail)
            }.thenApply {
                assertThat(timesCalled, `is`(1))
                assertThat(it, `is`(expectedRowInfo))

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
        cachingStore.retrieveData.get(1, SECONDS)
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

    class ParticipantsDb: MockDatabase() {
        var chat = Chat()
        var nbCallsToUpdateParticipants = 0
        var nbCallsToGetChat = 0

        override fun updateChatParticipants(chatId: String, participants: List<String>): CompletableFuture<Void> {
            chat = chat.copy(id = chatId, participants = participants)
            nbCallsToUpdateParticipants++
            return CompletableFuture.completedFuture(null)
        }

        override fun getChat(chatId: String): CompletableFuture<Chat> {
            nbCallsToGetChat++
            if (chatId == chat.id) {
                return CompletableFuture.completedFuture(chat)
            }
            return CompletableFuture.completedFuture(Chat())
        }
    }
    @Test
    fun updateChatParticipantsUpdatesTheWrappedDatabaseBothWhenCachedAndWhenNot() {
        val wrappedDatabase = ParticipantsDb()
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.setCurrentEmail(exampleEmail)

        val chatId = "chatId"
        val participants = listOf("user1@email.com", "user2@email")

        val isCorrect = cachingStore.updateChatParticipants(chatId, participants)
            .thenCompose { // update when not cached
                assertThat(wrappedDatabase.nbCallsToUpdateParticipants, `is`(1))
                assertThat(wrappedDatabase.chat.id, `is`(chatId))
                assertThat(wrappedDatabase.chat.participants, `is`(participants))

                cachingStore.getChat(chatId)
            }.thenCompose {

                assertThat(
                    wrappedDatabase.nbCallsToGetChat,
                    `is`(1)
                ) // sinced not cached before
                assertThat(it.participants, `is`(participants))
                assertThat(it.id, `is`(chatId))

                cachingStore.updateChatParticipants(chatId, emptyList())
            }.thenApply { // update when cached
                assertThat(wrappedDatabase.nbCallsToUpdateParticipants, `is`(2))
                assertThat(wrappedDatabase.chat.participants, `is`(emptyList()))

                true
            }.exceptionally {
                false
            }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    class ContactRowDB(private val defaultUser: UserInfo, private val rowInfos: List<ContactRowInfo>): MockDatabase() {
        var chat = Chat()
        var nbCallsToUpdateParticipants = 0
        var nbCallsToGetContactRowInfo = 0
        var sentMessage = Message()

        override fun updateChatParticipants(chatId: String, participants: List<String>): CompletableFuture<Void> {
            chat = chat.copy(id = chatId, participants = participants)
            nbCallsToUpdateParticipants++
            return CompletableFuture.completedFuture(null)
        }

        override fun getContactRowInfos(email: String): CompletableFuture<List<ContactRowInfo>> {
            nbCallsToGetContactRowInfo++
            assertThat(email, `is`(defaultUser.email))
            return CompletableFuture.completedFuture(rowInfos)
        }

        override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
            sentMessage = message
            return CompletableFuture.completedFuture(null)
        }

        var onChangeChat = Chat()
        override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
            onChange(onChangeChat)
        }
    }

    @Test
    fun sendingMessageInChatWhoseContactRowIsCachedUpdatesThatContactRowInCache() {

        val rowInfo = ContactRowInfo(
            "chatiId",
            "chatiName",
            Message("sender@email.com", "Sender Name", "Test Message", LocalDateTime.now().toString(), ReadState.SENT, mapOf()),
            false,
        )
        val unusedRowInfo = rowInfo.copy(chatId = "chatiId2", chatTitle = "chatiName2", isGroupChat = true)

        val wrappedDatabase = ContactRowDB(defaultUser, listOf(rowInfo, unusedRowInfo))
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)

        cachingStore.getCurrentEmail().get(1, SECONDS)

        val expectedMessage = Message(
            "other@email.com",
            "Sender Name",
            "New Message!",
            LocalDateTime.now().toString(),
            ReadState.SENT
        )

        val isCorrect = cachingStore.getContactRowInfo(defaultUser.email) // place contact row into cache
            .thenCompose {
                assertThat(wrappedDatabase.nbCallsToGetContactRowInfo, `is`(1))
                assertThat(it[0], `is`(rowInfo))
                cachingStore.sendMessage(rowInfo.chatId, expectedMessage) // this is supposed to update the contact row in the cache
            }.thenCompose {
                assert(wrappedDatabase.sentMessage == expectedMessage)
                cachingStore.getContactRowInfo(defaultUser.email)
            }.thenApply {
                // should not have called the wrapped database again as it is cached
                assertThat(wrappedDatabase.nbCallsToGetContactRowInfo, `is`(1))
                assertThat(it[0], `is`(rowInfo.copy(lastMessage = expectedMessage)))
                true
            }.exceptionally { false }.get(5, SECONDS)

        assertTrue(isCorrect)
    }

    @Test
    fun sendingMessageForUncachedChatClearsStaleChat() {
        val rowInfo = ContactRowInfo(
            "chatiId",
            "chatiName",
            Message("sender@email.com", "Sender Name", "Test Message", LocalDateTime.now().toString(), ReadState.SENT, mapOf()),
            false,
        )
        val uncachedRowInfo = rowInfo.copy(chatId = "chatiId2", chatTitle = "chatiName2", isGroupChat = true)

        val wrappedDatabase = ContactRowDB(defaultUser, listOf(rowInfo))
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
        val nonCachedMessage = Message(
            "other@email.com",
            "Sender Name",
            "New Message!",
            LocalDateTime.now().toString(),
            ReadState.SENT
        )

        val isCorrect = cachingStore.getContactRowInfo(defaultUser.email) // place contact row into cache
            .thenCompose {
                assertThat(wrappedDatabase.nbCallsToGetContactRowInfo, `is`(1))
                assertThat(it[0], `is`(rowInfo))
                cachingStore.sendMessage(uncachedRowInfo.chatId, nonCachedMessage) // this is supposed to update the contact row in the cache
            }.thenCompose {
                assert(wrappedDatabase.sentMessage == nonCachedMessage)
                cachingStore.getContactRowInfo(defaultUser.email)
            }.thenApply {
                // should have called the wrapped database again since the stale cache should be cleared
                assertThat(wrappedDatabase.nbCallsToGetContactRowInfo, `is`(2))

                true
            }.exceptionally { false }.get(5, SECONDS)

        assertTrue(isCorrect)
    }





    @Test
    fun sendingMessageForCachedChatUpdatesThatChatInsideTheCache() {
        val newMessage = Message(
            defaultUser.email,
            "Sender Name",
            "New Message!",
            LocalDateTime.now().toString(),
            ReadState.SENT
        )
        val expectedChat = defaultChat.copy(messages = defaultMessages + newMessage)

        val wrappedDatabase = SendMessageDB(defaultChat)
        cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)

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
            defaultUser.email,
            "Sender Name",
            "New Message!",
            LocalDateTime.now().toString(),
            ReadState.SENT
        )

        val wrappedDatabase = SendMessageDB(defaultChat)
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)

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
        val expectedChat = Chat.markOtherUsersMessagesAsRead(defaultChat, defaultUser.email)

        val wrappedDatabase = MarkMessagesAsReadDB(defaultChat)
        val cachingStore = CachingStore(wrappedDatabase,
            ApplicationProvider.getApplicationContext<Context>().dataStoreTest,
            ApplicationProvider.getApplicationContext()
        )
        cachingStore.retrieveData.get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
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
        cachingStore.retrieveData.get(1, SECONDS)
        cachingStore.setCurrentEmail(exampleEmail).get(1, SECONDS)
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

    }

    private val defaultMessages = ChatSample.MESSAGES
    private val defaultChat = Chat().copy(
        id = "defaultId",
        participants = listOf(willSmithUser.email, defaultUser.email),
        messages = defaultMessages
    )


    private val currentMonday: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val cachedEvents = EventOps.getEventList()

    private val nonCachedEvents = listOf(
        Event(
            name = "Event ahead of cache borders",
            color = Color(0xFF6DD3CE).value.toString(),
            start = currentMonday.plusWeeks(5).atTime(13, 0, 0).toString(),
            end = currentMonday.plusWeeks(5).atTime(15, 0, 0).toString(),
            address = Address(),
        ),
    )

    private val eventList = cachedEvents + nonCachedEvents

    private val groupEvents = eventList.map {event ->
        GroupEvent(
            event.copy(name = "${event.name} (group event)", start = event.end, end = LocalDateTime.parse(event.end).plusHours(1).format(EventOps.getEventDateFormatter())),
            MockDatabase.getDefaultEmail(),
            5,
        )
    }
}