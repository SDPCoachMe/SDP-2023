package com.github.sdpcoachme

// This test class is in the androidTest directory instead of Test directory because it uses
// MockDatabase which is in the androidTest directory.
// Otherwise we would have complicated dependencies.

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.firebase.database.CachingDatabase
import com.github.sdpcoachme.firebase.database.MockDatabase
import com.github.sdpcoachme.location.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.location.UserLocationSamples.Companion.NEW_YORK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS

class CachingDatabaseTest {

    private val exampleEmail = "example@email.com"

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
        val email = "oui@non.com"
        val user = UserInfo(
            "Will",
            "Smith",
            email,
            "0000000000",
            NEW_YORK,
            false,
            emptyList(),
            emptyList()
        )
        val retrievedUser = cachingDatabase.updateUser(user)
            .thenCompose { cachingDatabase.getUser(email) }
            .get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(email))
        assertEquals(user, retrievedUser)
    }

    @Test
    fun addUserOverridesPreviousValueInCache() {
        val wrappedDatabase = MockDatabase()
        val cachingDatabase = CachingDatabase(wrappedDatabase)
        val newUser = UserInfo(
            "John",
            "Doe",
            exampleEmail,
            "1234567890",
            LAUSANNE,
            false,
            emptyList(),
            emptyList()
        )
        val updatedUser = cachingDatabase.getUser(exampleEmail)
            .thenCompose {
                cachingDatabase.updateUser(newUser) }
            .thenCompose { cachingDatabase.getUser(exampleEmail) }
            .get(5, SECONDS)
        assertTrue(cachingDatabase.isCached(exampleEmail))
        assertEquals(newUser, updatedUser)
    }
}