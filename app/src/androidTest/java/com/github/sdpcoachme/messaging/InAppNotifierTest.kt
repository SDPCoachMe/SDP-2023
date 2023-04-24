package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.LoginActivity.TestTags.Buttons.Companion.SIGN_IN
import com.github.sdpcoachme.LoginActivity.TestTags.Companion.INFO_TEXT
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.MockDatabase
import com.github.sdpcoachme.location.UserLocationSamples
import com.github.sdpcoachme.map.MapActivity
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class InAppNotifierTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var database: Database

    private val toUser = UserInfo(
        "Jane",
        "Doe",
        "to@email.com",
        "0987654321",
        UserLocationSamples.LAUSANNE,
        true,
        emptyList(),
        emptyList()
    )

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

    @Before
    fun setup() {
        database = (getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(toUser)
        database.updateUser(currentUser)
    }

    @After
    fun tearDown() {
        if (database is MockDatabase) {
            (database as MockDatabase).restoreDefaultChatSetup()
            println("MockDatabase was torn down")
        }
    }

    @Test
    fun onMessageReceivedOpensChatActivityWhenTypeIsMessagingAndSenderIsKnown() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {

            sendAndClickOnNotification("Title", "Body", toUser.email, "messaging")

            // Check if ChatActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            composeTestRule.onNodeWithText(toUser.firstName + " " + toUser.lastName).assertExists()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertExists()
        }
    }
    
    @Test
    fun onMessageReceivedRedirectsToLoginActivityWhenCurrentUserEmailIsNotSet() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            database.setCurrentEmail("")
            sendAndClickOnNotification("Title", "Body", toUser.email, "messaging")

            // Check if LoginActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            composeTestRule.onNodeWithTag(INFO_TEXT, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(SIGN_IN, useUnmergedTree = true).assertExists()
        }
    } 

    /**
     * Helper function to click on a notification
     * 
     * @param expectedTitle The title of the notification
     * @param expectedBody The body of the notification
     */
    private fun sendAndClickOnNotification(expectedTitle: String, expectedBody: String, senderEmail: String, type: String) {
        val context = (getInstrumentation().targetContext.applicationContext as CoachMeApplication)
        InAppNotifier(context, database).onMessageReceived(
            expectedTitle,
            expectedBody,
            senderEmail,
            type
        )

        val device = UiDevice.getInstance(getInstrumentation())

        device.openNotification()
        device.wait(Until.hasObject(By.text(expectedTitle)), 5)
        val title: UiObject2 = device.findObject(By.text(expectedTitle))
        val text: UiObject2 = device.findObject(By.text(expectedBody))

        // check that the notification is present
        assertThat(title.text, `is`(expectedTitle))
        assertThat(text.text, `is`(expectedBody))

        text.click()
        device.waitForIdle() // Needed to wait for the activity to open
    }
}