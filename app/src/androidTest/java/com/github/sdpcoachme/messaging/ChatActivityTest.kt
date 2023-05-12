package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.graphics.Color
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
import androidx.test.uiautomator.UiDevice
import com.github.sdpcoachme.*
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.*
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChatActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val toUser = UserInfoSamples.COACH_1
    private lateinit var database: Database
    private var currentUser = UserInfoSamples.NON_COACH_1.copy(
        firstName = "Current_FirstName",
        lastName = "Current_LastName",
        chatContacts = listOf(toUser.email)
    )
    private val personalChatId = (currentUser.email + toUser.email)
    private val personalChatDefaultIntent = Intent(
        ApplicationProvider.getApplicationContext(), ChatActivity::class.java
    ).putExtra("chatId", personalChatId)

    private val groupChatId = "@@eventChatId"
    private val groupChatDefaultIntent = Intent(
        ApplicationProvider.getApplicationContext(), ChatActivity::class.java
    ).putExtra("chatId", groupChatId)
    private val toUser2 = UserInfoSamples.NON_COACH_2 // third participant for group chats
    private val wrappedEvent = Event(
        name = "Google I/O Keynote",
        color = Color(0xFFAFBBF2).value.toString(),
        start = LocalDate.now().plusDays(7).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0).toString(),
        end = LocalDate.now().plusDays(7).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0).toString(),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
    )
    private val groupEvent = GroupEvent(
        groupChatId,
        wrappedEvent,
        currentUser.email,
        5,
        listOf(currentUser.email, toUser.email, toUser2.email)
    )


    @Before
    fun setup() {
        database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(toUser).join()
        database.updateUser(currentUser).join()
        database.updateUser(toUser2).join()
        database.addGroupEvent(groupEvent, LocalDate.now().plusDays(7)).join()
        database.updateChatParticipants(groupChatId, listOf(currentUser.email, toUser.email, toUser2.email)).join()
    }

    @After
    fun tearDown() {
        if (database is MockDatabase) {
            (database as MockDatabase).restoreDefaultChatSetup()
            (database as MockDatabase).restoreDefaultAccountsSetup()
            (database as MockDatabase).restoreDefaultSchedulesSetup()
        }
    }

    @Test
    fun startingElementsArePresent() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            composeTestRule.onNodeWithTag(BACK).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertTextEquals(toUser.firstName + " " + toUser.lastName)
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).assertIsDisplayed()
        }
    }

    @Test
    fun whenScrolledToTheBottomScrollButtonIsNotDisplayed() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            // As the chat is opened with the "scroll" all the way at the bottom,
            // the scroll button should not be displayed when launching this activity
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun whenClickingScrollButtonScreenScrollsDown() {
        val msg1 = Message(toUser.email, "ToUser Name", "", LocalDateTime.now().toString())
        val msg2 = Message(currentUser.email, "CurrentUser Name", "", LocalDateTime.now().toString())

        database.sendMessage(personalChatId, msg1.copy(timestamp = LocalDateTime.now().minusDays(1).toString()))
        for (i in 0..20) {
            database.sendMessage(personalChatId, (msg1.copy(content = "toUser msg $i")))
            database.sendMessage(personalChatId, (msg2.copy(content = "currentUser msg $i")))
        }

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

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
    fun clickingOnContactRowInNormalChatOpensProfileOfThatContact() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
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
        println("current user: $currentUser")
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
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
            sleep(5000)

            composeTestRule.onNodeWithText("${UserInfoSamples.COACH_1.firstName} ${UserInfoSamples.COACH_1.lastName}")
                .assertIsDisplayed()
            Intents.release()
        }
    }

    @Test
    fun chatListenerAddedAtStartUp() {
        val mockDB = database as MockDatabase
        assertThat(mockDB.numberOfAddChatListenerCalls(), `is`(0))

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            assertThat(mockDB.numberOfAddChatListenerCalls(), `is`(1))
        }
    }

    @Test
    fun pressingBackButtonRemovesChatListener() {
        val mockDB = database as MockDatabase
        assertThat(mockDB.numberOfRemovedChatListenerCalls(), `is`(0))
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            val callsBeforeBack = mockDB.numberOfRemovedChatListenerCalls()
            device.pressBack()
            assertThat(mockDB.numberOfRemovedChatListenerCalls(), `is`(callsBeforeBack + 1))
        }
    }

    @Test
    fun whenOpeningChatWithNewContactThatContactIsAddedToTheUserInfoContactList() {
        currentUser = currentUser.copy(chatContacts = listOf())
        assertThat(currentUser.chatContacts, not(hasItem(toUser.email)))

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            val updatedUser = database.getUser(currentUser.email).get(5, TimeUnit.SECONDS)

            assertThat(updatedUser.chatContacts, hasItem(toUser.email))
        }
    }

    @Test
    fun whenReceivingAMessageFromAnExistingContactThatContactIsAddedToTheUserInfoContactList() {
        val user = currentUser.copy(chatContacts = listOf(toUser.email))
        assertThat(user.chatContacts, hasItem(toUser.email))
        database.updateUser(user)

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            val updatedUser = database.getUser(user.email).get(5, TimeUnit.SECONDS)

            assertThat(updatedUser.chatContacts, hasItem(toUser.email))
        }
    }

    @Test
    fun sendingMessagePlacesItInDbAndDisplaysItOnScreen() {
        val messageContent = "Send Message test!"

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL)
                .assertIsDisplayed()
                .performTextInput(messageContent)
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag(SEND)
                .assertIsDisplayed()
                .performClick()

            val chat = database.getChat(personalChatId).get()
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
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            database.addChatListener("run-previous-on-change") {}

            composeTestRule.onNodeWithText("test onChange method", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun messageSentByOtherUserDoesNotContainReadStateCheckMark() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

            database.sendMessage(personalChatId, Message(toUser.email, "Sender Name","Does not contain checkmark", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun messageSentByCurrentUserContainsReadStateCheckMark() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

            database.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun sentMarkIsDisplayedWhenMessageNotYetReceivedByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

            database.sendMessage(personalChatId, Message(currentUser.email, "Sender Name", "message", LocalDateTime.now().toString(), ReadState.SENT))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message sent icon")
        }
    }

    @Test
    fun receivedMarkIsDisplayedWhenMessageOnlyReceivedByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

            database.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString(), ReadState.RECEIVED))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message received icon")
        }
    }

    @Test
    fun readMarkIsDisplayedWhenMessageReadByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {

            database.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString(), ReadState.READ))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message read icon")
        }
    }


    
    @Test
    fun errorHandlerIsLaunchedIfCurrentUserEmailIsEmpty() {
        database.setCurrentEmail("")
        checkErrorPageIsLaunched(personalChatDefaultIntent)
    }

    @Test
    fun errorHandlerIsLaunchedIfToUserEmailNotPassedInIntentExtra() {
        val errorIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        checkErrorPageIsLaunched(errorIntent)
    }

    private fun checkErrorPageIsLaunched(chatIntent: Intent) {
        ActivityScenario.launch<ChatActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText("The Chat Interface did not receive the needed information for the chat.\n" +
                    "Please return to the login page and try again.")
                .assertIsDisplayed()
        }
    }

    @Test
    fun inAGroupChatTheChatNameIsDisplayedInTheContactField() {
        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(groupEvent.event.name)
        }
    }

    @Test
    fun nameOfSenderIsNotDisplayedIfSenderIsCurrentUser() {
        val message = Message(currentUser.email, "Not Displayed","message", LocalDateTime.now().toString(), ReadState.READ)
        database.sendMessage(groupEvent.groupEventId, message)

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            composeTestRule.onNodeWithText(message.content, substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(message.senderName, substring = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun nameOfSenderIsDisplayedIfSenderIsNotCurrentUser() {
        val message1 = Message(toUser.email, "Sender 1","msg content 1", LocalDateTime.now().toString(), ReadState.READ)
        val message2 = Message(toUser2.email, "Sender 2","msg content 2", LocalDateTime.now().toString(), ReadState.READ)
        val message3 = Message(currentUser.email, "Not Displayed","msg content 3", LocalDateTime.now().toString(), ReadState.READ)
        database.sendMessage(groupEvent.groupEventId, message1)
        database.sendMessage(groupEvent.groupEventId, message2)
        database.sendMessage(groupEvent.groupEventId, message3)

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            composeTestRule.onNodeWithText(message1.content, substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(message1.senderName, substring = true)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText(message2.content, substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(message2.senderName, substring = true)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText(message3.content, substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(message3.senderName, substring = true)
                .assertDoesNotExist()
        }
    }

    // TODO: complete this test once the event displaying activity is implemented
    @Test
    fun whenClickingOnTheContactFieldOfAnEventGroupChatTheEventIsDisplayed() {
        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(groupEvent.event.name)
                .performClick()

            // assert that the correct activity is launched here:
        }
    }

    @Test
    fun whenSendingMessageTheChatIsPlacedAtTheTopOfTheContactList() {
        val messageContent = "Send Message test!"

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL)
                .assertIsDisplayed()
                .performTextInput(messageContent)
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag(SEND)
                .assertIsDisplayed()
                .performClick()

            val chat = database.getChat(groupEvent.groupEventId).get()
            val message = chat.messages.last()
            assertThat(message.sender, `is`(currentUser.email))
            assertThat(message.content, `is`(messageContent))

            val timeSinceSend = Duration.between(LocalDateTime.parse(message.timestamp), LocalDateTime.now())
            assertThat(timeSinceSend.seconds, lessThan(5))

            composeTestRule.onNodeWithText(messageContent, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()

            for (userMail in groupEvent.participants) {
                database.getUser(userMail).thenApply { it.chatContacts[0] }.get().let {
                    assertThat(it, `is`(groupEvent.groupEventId))
                }
            }
        }
    }
}