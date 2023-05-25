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
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.*
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SEND
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_MESSAGE
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import com.github.sdpcoachme.profile.ProfileActivity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChatActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val toUser = UserInfoSamples.COACH_1
    private lateinit var store: CachingStore
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
        sport = Sports.TENNIS,
        address = Address(),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
    )
    private val groupEvent = GroupEvent(
        wrappedEvent,
        currentUser.email,
        5,
        listOf(currentUser.email, toUser.email, toUser2.email),
        groupChatId
    )


    @Before
    fun setup() {
        Intents.init()
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(currentUser.email).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(toUser).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(currentUser).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(toUser2).join()
        store.updateGroupEvent(groupEvent).join()
        store.updateChatParticipants(groupChatId, listOf(currentUser.email, toUser.email, toUser2.email)).join()
    }

    @After
    fun tearDown() {
        Intents.release()
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
    }

    @Test
    fun startingElementsArePresent() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)
            composeTestRule.onNodeWithTag(BACK).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertTextEquals(toUser.firstName + " " + toUser.lastName)
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).assertIsDisplayed()
        }
    }

    @Test
    fun whenScrolledToTheBottomScrollButtonIsNotDisplayed() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)
// As the chat is opened with the "scroll" all the way at the bottom,
            // the scroll button should not be displayed when launching this activity
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun whenClickingScrollButtonScreenScrollsDown() {
        val msg1 = Message(toUser.email, "ToUser Name", "", LocalDateTime.now().toString())
        val msg2 = Message(currentUser.email, "CurrentUser Name", "", LocalDateTime.now().toString())

        store.sendMessage(personalChatId, msg1.copy(timestamp = LocalDateTime.now().minusDays(1).toString())).get(1000, TimeUnit.MILLISECONDS)
        for (i in 0..20) {
            store.sendMessage(personalChatId, (msg1.copy(content = "toUser msg $i"))).get(1000, TimeUnit.MILLISECONDS)
            store.sendMessage(personalChatId, (msg2.copy(content = "currentUser msg $i"))).get(1000, TimeUnit.MILLISECONDS)
        }

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

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
            waitForLoading(it)

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
        }
    }

    @Test
    fun whenOpeningChatWithNewContactThatContactIsAddedToTheUserInfoContactList() {
        currentUser = currentUser.copy(chatContacts = listOf())
        assertThat(currentUser.chatContacts, not(hasItem(toUser.email)))

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            val updatedUser = store.getUser(currentUser.email).get(5, TimeUnit.SECONDS)

            assertThat(updatedUser.chatContacts, hasItem(toUser.email))
        }
    }

    @Test
    fun whenReceivingAMessageFromAnExistingContactThatContactIsAddedToTheUserInfoContactList() {
        val user = currentUser.copy(chatContacts = listOf(toUser.email))
        assertThat(user.chatContacts, hasItem(toUser.email))
        store.updateUser(user).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            val updatedUser = store.getUser(user.email).get(5, TimeUnit.SECONDS)

            assertThat(updatedUser.chatContacts, hasItem(toUser.email))
        }
    }

    @Test
    fun sendingMessagePlacesItInDbAndDisplaysItOnScreen() {
        val messageContent = "Send Message test!"

        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL)
                .assertIsDisplayed()
                .performTextInput(messageContent)
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag(SEND)
                .assertIsDisplayed()
                .performClick()

            val chat = store.getChat(personalChatId).get(1000, TimeUnit.MILLISECONDS)
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
            waitForLoading(it)

            store.addChatListener("run-previous-on-change") {}

            composeTestRule.onNodeWithText("test onChange method", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun messageSentByOtherUserDoesNotContainReadStateCheckMark() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            store.sendMessage(personalChatId, Message(toUser.email, "Sender Name","Does not contain checkmark", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun messageSentByCurrentUserContainsReadStateCheckMark() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            store.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString()))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun sentMarkIsDisplayedWhenMessageNotYetReceivedByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            store.sendMessage(personalChatId, Message(currentUser.email, "Sender Name", "message", LocalDateTime.now().toString(), ReadState.SENT))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message sent icon")
        }
    }

    @Test
    fun receivedMarkIsDisplayedWhenMessageOnlyReceivedByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            store.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString(), ReadState.RECEIVED))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message received icon")
        }
    }

    @Test
    fun readMarkIsDisplayedWhenMessageReadByRecipient() {
        ActivityScenario.launch<ChatActivity>(personalChatDefaultIntent).use {
            waitForLoading(it)

            store.sendMessage(personalChatId, Message(currentUser.email, "Sender Name","message", LocalDateTime.now().toString(), ReadState.READ))

            composeTestRule.onNodeWithTag(CHAT_MESSAGE.READ_STATE, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertContentDescriptionEquals("message read icon")
        }
    }

    @Test
    fun errorHandlerIsLaunchedIfChatIdNotPassedInIntentExtra() {
        val errorIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)

        ActivityScenario.launch<ChatActivity>(errorIntent).use {
            waitForLoading(it)

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
            waitForLoading(it)

            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(groupEvent.event.name)
        }
    }

    @Test
    fun nameOfSenderIsNotDisplayedIfSenderIsCurrentUser() {
        val message = Message(currentUser.email, "Not Displayed","message", LocalDateTime.now().toString(), ReadState.READ)
        store.sendMessage(groupEvent.groupEventId, message).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            waitForLoading(it)

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
        store.sendMessage(groupEvent.groupEventId, message1).get(1000, TimeUnit.MILLISECONDS)
        store.sendMessage(groupEvent.groupEventId, message2).get(1000, TimeUnit.MILLISECONDS)
        store.sendMessage(groupEvent.groupEventId, message3).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            waitForLoading(it)

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

    @Test
    fun whenClickingOnTheContactFieldOfAnEventGroupChatTheEventIsDisplayed() {
        store.updateGroupEvent(groupEvent).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            waitForLoading(it)

            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(groupEvent.event.name)
                .performClick()

            Intents.intended(
                hasComponent(GroupEventDetailsActivity::class.java.name)
            )
        }
    }

    @Test
    fun whenSendingMessageTheChatIsPlacedAtTheTopOfTheContactList() {
        val messageContent = "Send Message test!"

        ActivityScenario.launch<ChatActivity>(groupChatDefaultIntent).use {
            waitForLoading(it)

            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL)
                .assertIsDisplayed()
                .performTextInput(messageContent)
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag(SEND)
                .assertIsDisplayed()
                .performClick()

            val chat = store.getChat(groupEvent.groupEventId).get(1000, TimeUnit.MILLISECONDS)
            val message = chat.messages.last()
            assertThat(message.sender, `is`(currentUser.email))
            assertThat(message.content, `is`(messageContent))

            val timeSinceSend = Duration.between(LocalDateTime.parse(message.timestamp), LocalDateTime.now())
            assertThat(timeSinceSend.seconds, lessThan(5))

            composeTestRule.onNodeWithText(messageContent, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()

            for (userMail in groupEvent.participants) {
                store.getUser(userMail).thenApply { u -> u.chatContacts[0] }.get(1000, TimeUnit.MILLISECONDS).let {
                    id -> assertThat(id, `is`(groupEvent.groupEventId))
                }
            }
        }
    }

    // simulates push notifications where the user logs out after receiving the notification and then logs in
    @Test
    fun chatActivityRecoversFromMissingCurrentUserEmailIfItIsPassedInTheIntentExtra() {
        store.setCurrentEmail("").get(1000, TimeUnit.MILLISECONDS)
        val emailRecoveryIntent = personalChatDefaultIntent
            .putExtra("pushNotification_currentUserEmail", currentUser.email)

        ActivityScenario.launch<ChatActivity>(emailRecoveryIntent).use {
            waitForLoading(it)

            composeTestRule.onNodeWithTag(BACK).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CONTACT_FIELD.LABEL, useUnmergedTree = true).assertTextEquals(toUser.firstName + " " + toUser.lastName)
            composeTestRule.onNodeWithTag(CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).assertIsDisplayed()
        }
    }


    // Waits for the activity to finish loading any async state
    private fun waitForLoading(scenario: ActivityScenario<ChatActivity>) {
        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity {
            stateLoading = it.stateLoading
        }
        stateLoading.get(1000, TimeUnit.MILLISECONDS)
    }
}