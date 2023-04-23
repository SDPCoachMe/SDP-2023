package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.location.UserLocationSamples
import com.github.sdpcoachme.map.MapActivity
import com.google.android.gms.common.ConnectionResult.TIMEOUT
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test


class InAppNotifierTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

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

    @Test
    fun sendMessagingNotificationTest() {
        val context = (getInstrumentation().targetContext.applicationContext as CoachMeApplication)
        val database = context.database
        database.setCurrentEmail(currentUser.email)
        database.updateUser(currentUser)
        database.updateUser(toUser)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)

        ActivityScenario.launch<ChatActivity>(intent).use {
            Intents.init()
            InAppNotifier(context, database).sendMessagingNotification(
                "Title",
                "Body",
                toUser.email
            )

            val device = UiDevice.getInstance(getInstrumentation())

            device.openNotification()
            device.wait(Until.hasObject(By.text("Title")), 5)
            val title: UiObject2 = device.findObject(By.text("Title"))
            val text: UiObject2 = device.findObject(By.text("Body"))

            println("Title: ${title.text}")
            println("Text: ${text.text}")

            assertThat(title.text, `is`("Title"))
            assertThat(text.text, `is`("Body"))

            text.click()

//            Intents.intended(
//                allOf(
//                    IntentMatchers.hasComponent(ChatActivity::class.java.name),
//                    IntentMatchers.hasExtra("toUserEmail", toUser.email)
//                )
//            )

            // Intents.intended does not seem to work when clicking on a notification
            composeTestRule.onNodeWithText(toUser.firstName + " " + toUser.lastName).assertExists()

            Intents.release()
        }
    }
}