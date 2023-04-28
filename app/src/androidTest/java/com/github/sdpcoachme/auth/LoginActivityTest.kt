package com.github.sdpcoachme.auth

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.DELETE_ACCOUNT
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.SIGN_IN
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.SIGN_OUT
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Companion.INFO_TEXT
import com.github.sdpcoachme.data.UserLocationSamples
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.messaging.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Since those tests use UI Automator, we need to enable testTagsAsResourceId in the corresponding activity
// in order to be able to find the UI elements by their tags.
@RunWith(AndroidJUnit4::class)
open class LoginActivityTest {
    private val launchTimeout = 5000L
    private lateinit var device: UiDevice
    private lateinit var signedOutInfoText: String
    private lateinit var deleteInfoText: String

    @Before
    open fun startLoginActivityFromHomeScreen() {
        FirebaseAuth.getInstance().signOut()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        device = UiDevice.getInstance(instrumentation)

        device.pressHome()
        val launcherPackage: String = device.launcherPackageName
        ViewMatchers.assertThat(launcherPackage, CoreMatchers.notNullValue())

        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            launchTimeout
        )

        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        targetContext.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(targetContext.packageName).depth(0)),
            launchTimeout
        )

        // Get the strings from the resources
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        deleteInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "account_deleted",
                "string",
                context.packageName
            )
        )

        signedOutInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "signed_out",
                "string",
                context.packageName
            )
        )
    }

    @Test
    fun signOutOfGoogleAccountResultsInCorrectMessage() {
        val signOutButton = device.findObject(By.res(SIGN_OUT))
        signOutButton.click()

        device.wait(
            Until.hasObject(By.text(signedOutInfoText)),
            1000
        )

        ViewMatchers.assertThat(device.findObject(By.res(INFO_TEXT)).text, CoreMatchers.`is`(signedOutInfoText))
    }

    @Test
    fun deleteGoogleAccountResultsInCorrectMessage() {
        val deleteButton = device.findObject(By.res(DELETE_ACCOUNT))
        deleteButton.click()


        device.wait(
            Until.hasObject(By.text(deleteInfoText)),
            1000
        )

        ViewMatchers.assertThat(device.findObject(By.res(INFO_TEXT)).text, CoreMatchers.`is`(deleteInfoText))
    }

    //TODO implement this test
    // @Test
    fun signInLaunchesMapOnSuccess() {
        Intents.init()
        //val email = "patrick.sebastien@gmail.com"
        // perform here a successful login with the email
        intended(hasComponent(MapActivity::class.java.name))
        Intents.release()
    }

    class OpenChatTest: LoginActivityTest() {

        @get:Rule
        val composeTestRule = createEmptyComposeRule()

        lateinit var database: Database
        private val toUser = com.github.sdpcoachme.data.UserInfo(
            "Jane",
            "Doe",
            "to@email.com",
            "0987654321",
            UserLocationSamples.LAUSANNE,
            true,
            emptyList(),
            emptyList()
        )

        private val currentUser = com.github.sdpcoachme.data.UserInfo(
            "John",
            "Doe",
            "example@email.com",
            "0123456789",
            UserLocationSamples.NEW_YORK,
            false,
            emptyList(),
            emptyList()
        )
        @Before // done to enable testing without android ui
        override fun startLoginActivityFromHomeScreen() {
            database = (ApplicationProvider.getApplicationContext() as CoachMeApplication).database
            database.updateUser(currentUser)
            database.updateUser(toUser)
        }
        
        @Test
        fun whenOpenChatActivityActionAndSenderAndCurrentEmailIsSetTheChatActivityIsLaunched() {
            database.setCurrentEmail(currentUser.email)
            val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
                .putExtra("sender", toUser.email)
            intent.action = "OPEN_CHAT_ACTIVITY"

            ActivityScenario.launch<LoginActivity>(intent).use {

                // As the intent to open the chat activity is launched from within the onCreate
                // method, we cannot use Intents.intended(...) to check if the chat activity is launched
                composeTestRule.onNodeWithTag(ChatActivity.TestTags.CONTACT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
                composeTestRule.onNodeWithTag(ChatActivity.TestTags.CHAT_FIELD.LABEL, useUnmergedTree = true).assertIsDisplayed()
                composeTestRule.onNodeWithText(toUser.firstName + " " + toUser.lastName).assertIsDisplayed()
            }
        }

        @Test
        fun whenOpenChatActivityActionAndSenderSetButCurrentEmailIsNotSetWeStayInLoginActivity() {
            database.setCurrentEmail("")
            val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
                .putExtra("sender", toUser.email)
            intent.action = "OPEN_CHAT_ACTIVITY"

            ActivityScenario.launch<LoginActivity>(intent).use {

                // As the intent to open the chat activity is launched from within the onCreate
                // method, we cannot use Intents.intended(...) to check if the chat activity is launched
                composeTestRule.onNodeWithTag(SIGN_IN, useUnmergedTree = true).assertIsDisplayed()
                composeTestRule.onNodeWithTag(INFO_TEXT, useUnmergedTree = true).assertIsDisplayed()
            }
        }

        @Test
        fun whenOpenChatActivityActionSetButSenderNotSetWeStayInLoginActivity() {
            database.setCurrentEmail("")
            val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
            intent.action = "OPEN_CHAT_ACTIVITY"

            ActivityScenario.launch<LoginActivity>(intent).use {

                // As the intent to open the chat activity is launched from within the onCreate
                // method, we cannot use Intents.intended(...) to check if the chat activity is launched
                composeTestRule.onNodeWithTag(SIGN_IN, useUnmergedTree = true).assertIsDisplayed()
                composeTestRule.onNodeWithTag(INFO_TEXT, useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }
}