package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.database.CachingStore
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
import java.util.concurrent.TimeUnit


class InAppNotifierTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var store: CachingStore

    private val toUser = UserInfoSamples.COACH_1

    private val currentUser = UserInfoSamples.NON_COACH_1

    private val personalChatId = Chat.chatIdForPersonalChats(toUser.email, currentUser.email)

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(currentUser.email).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(toUser).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(currentUser).get(1000, TimeUnit.MILLISECONDS)
    }

    @After
    fun tearDown() {
        (getInstrumentation().targetContext.applicationContext as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
    }

    @Test
    fun onMessageReceivedOpensChatActivityWhenTypeIsMessagingAndChatIdIsKnown() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        // The start screen can be anything, MapActivity is just used here
        ActivityScenario.launch<MapActivity>(intent).use {

            sendNotification("Title", "Body", personalChatId, "messaging")
            clickOnNotification("Title", "Body")

            // Check if ChatActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            // TODO: would need to wait for the state to load before checking the UI...
            composeTestRule.onNodeWithText(toUser.firstName + " " + toUser.lastName).assertExists()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun onMessageReceivedRedirectsToCoachesListActivityWhenChatIdIsNotSet() {
        val context = ApplicationProvider.getApplicationContext() as CoachMeApplication
        val intent = Intent(context, MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification("Title", "Body", "", "messaging")
            clickOnNotification("Title", "Body")

            // Check if CoachesListActivity is opened
            // Intents.intended does not seem to work when clicking on a notification
            // TODO: would need to wait for the state to load before checking the UI...
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assert(hasText(context.getString(R.string.chats)))

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
        val context = ApplicationProvider.getApplicationContext() as CoachMeApplication
        val intent = Intent(context, MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification(null, null, null, "messaging")
            clickOnNotification("New message", "You have a new message")


            // Since the ChatId is not set, clicking on the notification should take the user to their contacts
            // Since the sender is not set, clicking on the notification should take the user to their contacts
            // TODO: would need to wait for the state to load before checking the UI...
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assert(hasText(context.getString(R.string.chats)))

            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
        }
    }

    // This test checks that the emails of the users can be recovered when receiving a push notification while the app is
    // open, then logging out and closing the application. After closing the application, the notification is pressed.
    // This was a bugfix as, before, the app would crash since the email of the user was no longer stored in the app.
    // Thanks to this test, we can ensure that the email still can be recovered even after having logged out so that
    // clicking on the notification does not result in an error.
    @Test
    fun onMessageReceivedEnablesEmailRecoveryAndOpensChatActivityWhenUserReceivesNotificationThenLoggsOutAndThenClicksOnIt() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        // The start screen can be anything, MapActivity is just used here
        ActivityScenario.launch<MapActivity>(intent).use {

            sendNotification("Title", "Body", personalChatId, "messaging")


            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER, useUnmergedTree = true)
                .assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.Buttons.HAMBURGER_MENU, useUnmergedTree = true)
                .performClick()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER, useUnmergedTree = true)
                .assertIsDisplayed()

            val device = UiDevice.getInstance(getInstrumentation())
            device.waitForIdle()
            device.findObject(
                UiSelector().text("Log out"))
                .click()

            // exit application
            device.pressHome()

            clickOnNotification("Title", "Body")

            // Check if ChatActivity is opened
            composeTestRule.onNodeWithText(toUser.firstName + " " + toUser.lastName).assertExists()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertExists()
        }
    }

    // This test checks the same email recovery as the previous test, however, this time the notification
    // does not contain the chat id which means that the expected activity to be opened is the contacts listing.
    @Test
    fun onMessageReceivedEnablesEmailRecoveryAndOpensContactsListWhenUserReceivesNotificationWithoutChatIdThenLoggsOutAndThenClicksOnIt() {
        val context = ApplicationProvider.getApplicationContext() as CoachMeApplication
        val intent = Intent(context, MapActivity::class.java)

        ActivityScenario.launch<MapActivity>(intent).use {
            sendNotification("Title", "Body", "", "messaging")

            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER, useUnmergedTree = true)
                .assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.Buttons.HAMBURGER_MENU, useUnmergedTree = true)
                .performClick()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER, useUnmergedTree = true)
                .assertIsDisplayed()

            val device = UiDevice.getInstance(getInstrumentation())
            device.waitForIdle()
            device.findObject(UiSelector().text("Log out"))
                .click()

            // exit application
            device.pressHome()

            clickOnNotification("Title", "Body")

            // Check if CoachesListActivity is opened
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.BAR_TITLE).assert(hasText(context.getString(R.string.chats)))

            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
        }
    }

    private fun sendNotification(expectedTitle: String?, expectedBody: String?, chatId: String?, type: String?) {
        val context = (getInstrumentation().targetContext.applicationContext as CoachMeApplication)
        InAppNotifier(context, store).sendNotification(
            expectedTitle,
            expectedBody,
            chatId,
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