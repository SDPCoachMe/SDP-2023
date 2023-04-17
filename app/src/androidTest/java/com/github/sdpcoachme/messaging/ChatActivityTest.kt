package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.*
import com.github.sdpcoachme.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.firebase.database.MockDatabase
import com.github.sdpcoachme.location.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.location.UserLocationSamples.Companion.NEW_YORK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ChatActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)

    private val toUser = UserInfo(
        "Jane",
        "Doe",
        "to@email.com",
        "0987654321",
        LAUSANNE,
        true,
        emptyList(),
        emptyList()
    )
    private lateinit var database: Database
    private val currentUser = UserInfo(
        "John",
        "Doe",
        "example@email.com",
        "0123456789",
        NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )
    private val chatId = (currentUser.email + toUser.email)


    @Before
    fun setup() {
        database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(toUser)
    }

    @After
    fun tearDown() {
        if (database is MockDatabase) {
            (database as MockDatabase).restoreDefaultChatSetup()
            println("MockDatabase was torn down")
        }
    }

    @Test
    fun startingElementsArePresent() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(BACK).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertTextEquals(toUser.firstName + " " + toUser.lastName)
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).assertIsDisplayed()
        }
    }

    @Test
    fun whenScrolledToTheBottomScrollButtonIsNotDisplayed() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            // As the chat is opened with the "scroll" all the way at the bottom,
            // the scroll button should not be displayed when launching this activity
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun whenClickingScrollButtonScreenScrollsDown() {
        val msg1 = Message(toUser.email, "", LocalDateTime.now().toString())
        val msg2 = Message(currentUser.email, "", LocalDateTime.now().toString())

        database.sendMessage(chatId, msg1.copy(timestamp = LocalDateTime.now().minusDays(1).toString()))
        for (i in 0..20) {
            database.sendMessage(chatId, (msg1.copy(content = "toUser msg $i")))
            database.sendMessage(chatId, (msg2.copy(content = "currentUser msg $i")))
        }

        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()

            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).performTouchInput { swipeDown() }
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertIsDisplayed()
                .performClick()
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
            // as the scrolling is done by just switching a boolean, we also test it when switching it
            // the other way around
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).performTouchInput { swipeDown() }
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertIsDisplayed()
                .performClick()
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun clickingOnContactRowOpensProfileOfThatContact() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            Intents.init()

            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(
                allOf(
                    hasComponent(ProfileActivity::class.java.name),
                    hasExtra("email", toUser.email),
                    hasExtra("isViewingCoach", true)
                )
            )
            Intents.release()
        }
    }

    @Test
    fun backButtonReturnsToListedContactsWhenPressed() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            Intents.init()

            composeTestRule.onNodeWithTag(BACK)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(
                allOf(
                    hasComponent(CoachesListActivity::class.java.name),
                    hasExtra("isViewingContacts", true)
                )
            )

            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
            Intents.release()
        }
    }

    @Test
    fun sendingMessagePlacesItInDbAndDisplaysItOnScreen() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)
        val messageContent = "Send Message test!"

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL)
                .assertIsDisplayed()
                .performTextInput(messageContent)
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag(SEND)
                .assertIsDisplayed()
                .performClick()

            val chat = database.getChat(chatId).get()
            val message = chat.messages.last()
            assertThat(message.sender, `is`(currentUser.email))
            assertThat(message.content, `is`(messageContent))

            val timeSinceSend = Duration.between(LocalDateTime.parse(message.timestamp), LocalDateTime.now())
            assertThat(timeSinceSend.seconds, lessThan(1))

            composeTestRule.onNodeWithText(messageContent, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun whenOnChangeCalledWithNewChatMessageChatIsUpdated() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            database.addChatListener("run-previous-on-change") {}

            composeTestRule.onNodeWithText("test onChange method", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun messageSentByOtherUserDoesNotContainIsReadCheckMark() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {

            database.sendMessage(chatId, Message(toUser.email, "", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.IS_READ, useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun messageSentByCurrentUserContainsIsReadCheckMark() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {

            database.sendMessage(chatId, Message(currentUser.email, "message", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.IS_READ, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun notReadMarkIsDisplayedWhenMessageNotYetReadByRecipient() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {

            database.sendMessage(chatId, Message(currentUser.email, "message", LocalDateTime.now().toString(), false))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.IS_READ, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("Not read by recipient icon")
        }
    }

    @Test
    fun readMarkIsDisplayedWhenMessageNotYetReadByRecipient() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {

            database.sendMessage(chatId, Message(currentUser.email, "message", LocalDateTime.now().toString(), true))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.IS_READ, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("read by recipient icon")
        }
    }
    
    @Test
    fun errorHandlerIsLaunchedIfCurrentUserEmailIsEmpty() {
        database.setCurrentEmail("")

        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)
        checkErrorPageIsLaunched(chatIntent)
    }

    @Test
    fun errorHandlerIsLaunchedIfToUserEmailNotPassedInIntentExtra() {
        checkErrorPageIsLaunched(defaultIntent)
    }

    private fun checkErrorPageIsLaunched(chatIntent: Intent) {
        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText("The Chat Interface did not receive both needed users.\nPlease return to the login page and try again.")
                .assertIsDisplayed()
        }
    }

    @Test
    fun dashboardHasRightTitleOnChat() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)
        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText("Coach Me"))
        }
    }
    @Test
    fun dashboardIsAccessibleAndDisplayableFromChat() {
        val chatIntent = defaultIntent.putExtra("toUserEmail", toUser.email)
        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }
    }
}