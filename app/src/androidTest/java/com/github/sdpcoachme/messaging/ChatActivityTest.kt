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
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.MESSAGE_COACH
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.time.Duration
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ChatActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val toUser = UserInfo(
        "Jane",
        "Doe",
        "to@email.com",
        "0987654321",
        "Bernstrasse 10, 3114 Wichtrach",
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
        "Thunstrasse 10, 3114 Wichtrach",
        false,
        emptyList(),
        emptyList()
    )
    private val chatId = (currentUser.email + toUser.email)


    @Before
    fun setup() {
        database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.currentUserEmail = currentUser.email
        database.addUser(toUser)
    }

    @Test
    fun startingElementsArePresent() {
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(BACK).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertTextEquals(toUser.firstName + " " + toUser.lastName)
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).assertIsDisplayed()
        }
    }

    @Test
    fun whenScrolledToTheBottomScrollButtonIsNotDisplayed() {
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
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

        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

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
        val chatIntent =
            Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

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
    fun backButtonReturnsToCoachesListWhenComingFromThere() {
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)

        ActivityScenario.launch<CoachesListActivity>(chatIntent).use {
            Intents.init()

            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(MESSAGE_COACH)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(
                allOf(
                    hasComponent(ChatActivity::class.java.name),
                    hasExtra("toUserEmail", toUser.email)
                )
            )

            composeTestRule.onNodeWithTag(BACK)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(
                hasComponent(CoachesListActivity::class.java.name),
            )

            Intents.release()
        }
    }

    @Test
    fun backButtonReturnsToContactsListActivityWhenComingFromThere() {
        val contactsIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
        contactsIntent.putExtra("isViewingContacts", true)

        ActivityScenario.launch<CoachesListActivity>(contactsIntent).use {
            Intents.init()
            sleep(3000)
            composeTestRule.onNodeWithText("${toUser.firstName} ${toUser.lastName}")
                .assertIsDisplayed()
                .performClick()

            sleep(3000)

            Intents.intended(
                allOf(
                    hasComponent(ChatActivity::class.java.name),
                    hasExtra("toUserEmail", toUser.email)
//                    hasExtra("isViewingContacts", true)
                )
            )

            sleep(3000)
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
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)
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
        //when on change called with new chat message chat is updated
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            database.addChatListener("run-previous-on-change") {}

            composeTestRule.onNodeWithText("test onChange method", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }
    
    @Test
    fun errorHandlerIsLaunchedIfCurrentUserEmailIsEmpty() {
        database.currentUserEmail = ""

        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)
        checkErrorPageIsLaunched(chatIntent)
    }

    @Test
    fun errorHandlerIsLaunchedIfToUserEmailNotPassedInIntentExtra() {
        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        checkErrorPageIsLaunched(chatIntent)
    }

    private fun checkErrorPageIsLaunched(chatIntent: Intent) {
        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText("The Chat Interface did not receive both needed users.\nPlease return to the login page and try again.")
                .assertIsDisplayed()
        }
    }
}