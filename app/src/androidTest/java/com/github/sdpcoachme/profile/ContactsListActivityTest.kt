package com.github.sdpcoachme.profile

import android.content.Intent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.CoachesListActivity.TestTags.Buttons.Companion.FILTER
import com.github.sdpcoachme.profile.CoachesListActivityTest.Companion.populateDatabase
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ContactsListTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val store = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).store

    private lateinit var scenario: ActivityScenario<CoachesListActivity>

    private val defaultEmail = "example@email.com"
    private val othersMessage = Message(
        UserInfoSamples.COACH_1.email,
        "Other's Name",
        "Test received message",
        LocalDateTime.now().toString()
    )
    private val ownMessage = Message(defaultEmail,
        "Current Name",
        "Test sent message",
        LocalDateTime.now().toString()
    )

    @Before
    fun setup() {
        // Launch the activity
        populateDatabase(store).join()
    }

    // Needed to allow populating the database with more elements before launching the activity
    private fun startActivity() {
        val contactIntent =
            Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
        contactIntent.putExtra("isViewingContacts", true)
        scenario = ActivityScenario.launch(contactIntent)

        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity {
            stateLoading = it.stateLoading
        }
        stateLoading.get(1000, TimeUnit.MILLISECONDS)
        Intents.init()
    }

    @After
    fun cleanup() {
        scenario.close()
        store.clearCache()
        Intents.release()
    }

    @Test
    fun whenViewingContactsTheRecipientsNameIsDisplayed() {
        startActivity()
        composeTestRule.onNodeWithText("${UserInfoSamples.COACH_1.firstName} ${UserInfoSamples.COACH_1.lastName}")
            .assertIsDisplayed()
    }

    @Test
    fun whenViewingContactsTheLastMessageAndTheSenderNameIsDisplayedWhenNotSentByCurrentUser() {
        store.sendMessage("chatId", othersMessage.copy(content = "Shouldn't be displayed")).get(1000, TimeUnit.MILLISECONDS)
        store.sendMessage("chatId", othersMessage).get(1000, TimeUnit.MILLISECONDS)
        startActivity()

        composeTestRule.onNodeWithText("${othersMessage.senderName}: ${othersMessage.content}")
            .assertIsDisplayed()
    }

    @Test
    fun whenViewingContactsTheLastMessageOfTheChatAndYouIsDisplayedWhenSentByCurrentUser() {
        store.sendMessage("chatId", ownMessage.copy(content = "Shouldn't be displayed")).get(1000, TimeUnit.MILLISECONDS)
        store.sendMessage("chatId", ownMessage).get(1000, TimeUnit.MILLISECONDS)
        startActivity()

        composeTestRule.onNodeWithText("You: ${ownMessage.content}")
            .assertIsDisplayed()
    }

    @Test
    fun whenViewingContactWhereNoMessageHasBeenSentYetDefaultMessagePromptIsShown() {
        // send a message to make sure only the other default message is displayed
        // (i.e., so onNodeWithText only finds one instance)
        store.sendMessage("chatId", othersMessage.copy(content = "Won't be accounted for by the test")).get(1000, TimeUnit.MILLISECONDS)
        startActivity()
        composeTestRule.onNodeWithText("Tap to write a message")
            .assertIsDisplayed()
    }

    @Test
    fun whenViewingContactsAndClickingOnClientChatActivityIsLaunched() {
        startActivity()
        val coach = UserInfoSamples.COACH_1

        composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
            .assertIsDisplayed()
            .performClick()

        val expectedChatId =
            if (defaultEmail < coach.email) "$defaultEmail${coach.email}" else "${coach.email}$defaultEmail"
        // Check that the ChatActivity is launched with the correct extras
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasComponent(ChatActivity::class.java.name),
                IntentMatchers.hasExtra("chatId", expectedChatId),
            )
        )
    }

    @Test
    fun dashboardHasRightTitleOnContactsList() {
        startActivity()
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.chats)
        composeTestRule.onNodeWithTag(BAR_TITLE).assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
    }

    @Test
    fun dashboardIsAccessibleAndDisplayableFromContactsList() {
        startActivity()
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun filteringButtonIsNotShownInContactsList() {
        startActivity()
        composeTestRule.onNodeWithTag(FILTER)
            .assertDoesNotExist()
    }
}