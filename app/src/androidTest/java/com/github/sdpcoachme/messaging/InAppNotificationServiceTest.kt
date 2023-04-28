package com.github.sdpcoachme.messaging

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocationSamples
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LOCATION
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PHONE
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.RemoteMessage
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture

@RunWith(AndroidJUnit4::class)
class InAppNotificationServiceTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val currentUser = UserInfo(
        "John",
        "Doe",
        "example@email.com",
        "0123456789",
        UserLocationSamples.NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )

    // This test does not work in the ci pipeline,
    @Test
    fun addFcmTokenToDatabasePlacesTokenIntoTheDb() {
        val database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(currentUser)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        FirebaseApp.initializeApp(appContext)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            val expectedEmail = "test@email.com"
            var receivedEmail = ""
            var receivedToken = ""
            val future = CompletableFuture<Void>()

            // Created a "new" database to use a future to know when the token is added to the database
            // (otherwise we would need to wait an unknown amount of time before testing)
            class TestDB : MockDatabase() {
                override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
                    receivedEmail = email
                    receivedToken = token
                    future.complete(null)
                    return future
                }
            }
            val db = TestDB()
            db.setCurrentEmail(expectedEmail)

            InAppNotificationService.addFCMTokenToDatabase(db)

            val result = future.thenApply {
                assertThat(receivedEmail, `is`(expectedEmail))
                assertTrue(receivedToken.isNotEmpty()) // as the tokens depend on the device, we can't check for a specific value
                true
            }.exceptionally {
                false
            }.get(10, java.util.concurrent.TimeUnit.SECONDS) // added so we can check if the tests in the future succeed or not

            assertThat(result, `is`(true))
        }
    }

    @Test
    fun onMessageReceivedWithNullArgumentDoesNothing() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = (context as CoachMeApplication).database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(currentUser)

        val intent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)

        ActivityScenario.launch<ProfileActivity>(intent).use {
            val message = RemoteMessage.Builder("to").build()
            InAppNotificationService().onMessageReceived(message)

            // Check that we're still in the ProfileActivity (done by checking that the tags are present)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(PHONE, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    // Since it is not possible to create instances of RemoteMessage, we can't test the onMessageReceived method.
}