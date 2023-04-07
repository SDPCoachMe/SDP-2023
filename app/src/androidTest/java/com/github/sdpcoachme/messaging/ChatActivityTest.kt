package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.DashboardActivity
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.BACK
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Buttons.Companion.SCROLL_TO_BOTTOM
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_BOX
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CHAT_FIELD
import com.github.sdpcoachme.messaging.ChatActivity.TestTags.Companion.CONTACT_FIELD
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        false,
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
    private val chatId = (currentUser.email + toUser.email).replace(".", ",")



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

        ActivityScenario.launch<DashboardActivity>(chatIntent).use {
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

        ActivityScenario.launch<DashboardActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()

        }
    }

    @Test
    fun whenClickingScrollButtonScreenScrollsDown() {
        val msg1 = Message(toUser.email, "", LocalDateTime.now().toString())
        val msg2 = Message(currentUser.email, "", LocalDateTime.now().toString())

        for (i in 0..20) {
            database.sendMessage(chatId, (msg1.copy(content = "toUser msg $i")))
            database.sendMessage(chatId, (msg2.copy(content = "currentUser msg $i")))
        }

        val chatIntent = Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java)
        chatIntent.putExtra("toUserEmail", toUser.email)

        ActivityScenario.launch<DashboardActivity>(chatIntent).use {
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()

            composeTestRule.onNodeWithTag(CHAT_BOX.CONTAINER).performTouchInput { swipeDown() }
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertIsDisplayed()
                .performClick()
            composeTestRule.onNodeWithTag(SCROLL_TO_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        }
    }


}