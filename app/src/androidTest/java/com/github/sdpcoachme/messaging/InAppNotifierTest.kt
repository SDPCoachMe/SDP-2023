package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.SIGN_IN
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Companion.INFO_TEXT
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocationSamples
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.ui.Dashboard
import junit.framework.TestCase.assertNull
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
        database = (getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
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

        // The start screen can be anything, MapActivity is just used here
        ActivityScenario.launch<MapActivity>(intent).use {

            sendNotification("Title", "Body", toUser.email, "messaging")
            clickOnNotification("Title", "Body")

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
            sendNotification("Title", "Body", toUser.email, "messaging")
            clickOnNotification("Title", "Body")

            // Check if LoginActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            composeTestRule.onNodeWithTag(INFO_TEXT, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(SIGN_IN, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun onMessageReceivedRedirectsToCoachesListActivityWhenSenderIsNotSet() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification("Title", "Body", "", "messaging")
            clickOnNotification("Title", "Body")

            // Check if CoachesListActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            // make sure "Contacts" is displayed in the header bar
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assert(hasText("Contacts"))

            composeTestRule.onNodeWithText(toUser.location.address).assertIsDisplayed()
            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun onMessageReceivedDoesNothingIfMessageTypeIsNotMessaging() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification("Title", "Body", "", null)

            val device = UiDevice.getInstance(getInstrumentation())

            device.openNotification()
            device.wait(Until.hasObject(By.text("Title")), 5)
            val title = device.findObject(By.text("Title"))
            val body = device.findObject(By.text("Body"))

            // Check that this notification has not arrived
            assertNull(title)
            assertNull(body)

            // Close the notification drawer
            device.pressBack()
        }
    }

    @Test
    fun defaultNotificationIsShownIfOnlyNotificationTypeIsSet() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification(null, null, null, "messaging")
            clickOnNotification("New message", "You have a new message")


            // Since the sender is not set, clicking on the notification should take the user to their contacts
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assert(hasText("Contacts"))

            composeTestRule.onNodeWithText(toUser.location.address).assertIsDisplayed()
            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
        }
    }

    private fun sendNotification(expectedTitle: String?, expectedBody: String?, senderEmail: String?, type: String?) {
        val context = (getInstrumentation().targetContext.applicationContext as CoachMeApplication)
        InAppNotifier(context, database).sendNotification(
            expectedTitle,
            expectedBody,
            senderEmail,
            type
        )
    }

    /**
     * Helper function to click on a notification
     * 
     * @param expectedTitle The title of the notification
     * @param expectedBody The body of the notification
     */
    private fun clickOnNotification(expectedTitle: String?, expectedBody: String?) {
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