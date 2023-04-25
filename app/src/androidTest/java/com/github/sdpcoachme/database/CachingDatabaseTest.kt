package com.github.sdpcoachme.database

// This test class is in the androidTest directory instead of Test directory because it uses
// MockDatabase which is in the androidTest directory.
// Otherwise we would have complicated dependencies.

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserLocationSamples.Companion.NEW_YORK
import com.github.sdpcoachme.data.UserInfo
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import com.github.sdpcoachme.firebase.database.CachingDatabase
import com.github.sdpcoachme.firebase.database.MockDatabase
import junit.framework.TestCase.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS
import java.time.DayOfWeek
import java.time.LocalDate
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
    fun addEventsToUserPutsUserInCacheAndUpdatesEvents() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val addUser = cachingDatabase.updateUser(willSmithUser)
            .thenApply { cachingDatabase.clearCache() }

        addUser.thenCompose { cachingDatabase.addEventsToUser(willSmithUser.email, eventList) }
            .get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(willSmithUser.email))

        val retrievedUser = cachingDatabase.getUser(willSmithUser.email)
            .get(5, SECONDS)
        assertEquals(willSmithUser.copy(events = eventList), retrievedUser)
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


    val exampleEmail = "example@email.com"

    val defaultUser = UserInfo(
        "John",
        "Doe",
        exampleEmail,
        "1234567890",
        LAUSANNE,
        false,
        emptyList(),
        emptyList()
    )

    val willSmithUser = UserInfo(
        "Will",
        "Smith",
        "oui@non.com",
        "0000000000",
        NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )

    val rogerFedererUser = UserInfo(
        "Roger",
        "Federer",
        "roger@federer.com",
        "1111111111",
        LAUSANNE,
        true,
        emptyList(),
        emptyList()
    )

    val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private val eventList = listOf(
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



}