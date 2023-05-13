package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.data.UserAddressSamples
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.ADDRESS
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PHONE
import com.google.firebase.messaging.RemoteMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InAppNotificationServiceTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val currentUser = UserInfo(
        "John",
        "Doe",
        "example@email.com",
        "0123456789",
        UserAddressSamples.NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )

    private lateinit var store: CachingStore

    @Before
    fun setup() {
        store = (ApplicationProvider.getApplicationContext() as CoachMeApplication).store
    }

    @Test
    fun onMessageReceivedWithNullArgumentDoesNothing() {
        store.setCurrentEmail(currentUser.email).get(1000, TimeUnit.SECONDS)
        store.updateUser(currentUser).get(1000, TimeUnit.SECONDS)

        val intent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)

        ActivityScenario.launch<ProfileActivity>(intent).use {
            waitForLoading(it)

            val message = RemoteMessage.Builder("to").build()
            InAppNotificationService().onMessageReceived(message)

            // Check that we're still in the ProfileActivity (done by checking that the tags are present)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(PHONE, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    // Since it is not possible to create instances of RemoteMessage, we cannot test the onMessageReceived method.


    // Waits for the activity to finish loading any async state
    private fun waitForLoading(scenario: ActivityScenario<ProfileActivity>) {
        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity {
            stateLoading = it.stateUpdated
        }
        stateLoading.get(1000, TimeUnit.MILLISECONDS)
    }
}