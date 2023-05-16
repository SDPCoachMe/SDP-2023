package com.github.sdpcoachme.auth

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.auth.LoginActivity.TestTags.Buttons.Companion.LOG_IN
import com.github.sdpcoachme.data.UserAddressSamples
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.messaging.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

// Since those tests use UI Automator, we need to enable testTagsAsResourceId in the corresponding activity
// in order to be able to find the UI elements by their tags.
@RunWith(AndroidJUnit4::class)
open class LoginActivityTest {
    private val launchTimeout = 5000L
    private lateinit var device: UiDevice

    @Before // done to enable testing without android ui
    fun setup() {
        store = (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun startFromHomeScreenLaunchesLoginActivity() {
        FirebaseAuth.getInstance().signOut()
        val instrumentation = getInstrumentation()
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

        // Easiest way to verify that the login activity is launched: check if the login button is displayed
        Assert.assertNotNull(device.wait(
            Until.hasObject(By.res(LOG_IN)),
            2000
        ))
        pressBackUnconditionally()
    }

    lateinit var store: CachingStore
    private val toUser = UserInfo(
        "Jane",
        "Doe",
        "to@email.com",
        "0987654321",
        UserAddressSamples.LAUSANNE,
        true,
        emptyList(),
        emptyList()
    )

    private val currentUser = UserInfo(
        "John",
        "Doe",
        "example@email.com",
        "0123456789",
        UserAddressSamples.NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )

    private val nonExistingUser = UserInfo(
        "",
        "",
        "nonexisting@email.com",
        "",
        UserAddressSamples.NEW_YORK,
        false,
        emptyList(),
        emptyList()
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun whenNotLoggedInNoRedirection() {
        store.setCurrentEmail("").get(1000, TimeUnit.MILLISECONDS)
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)

        ActivityScenario.launch<LoginActivity>(intent).use {
            waitForLoading(it)

            // Assert that we are still in the login activity
            composeTestRule.onNodeWithTag(LOG_IN, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun whenExistingUserLoggedInWithNoActionSetRedirectToMapActivity() {
        store.updateUser(currentUser).get(1000, TimeUnit.MILLISECONDS)
        store.setCurrentEmail(currentUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)

        ActivityScenario.launch<LoginActivity>(intent).use {
            waitForLoading(it)

            // Assert that we launched the map activity
            intended(hasComponent(MapActivity::class.java.name))
        }
        pressBackUnconditionally()
    }

    @Test
    fun whenExistingUserLoggedInWithOpenChatActivityActionSetRedirectToChatActivity() {
        store.updateUser(currentUser).get(1000, TimeUnit.MILLISECONDS)
        store.updateUser(toUser).get(1000, TimeUnit.MILLISECONDS)
        store.setCurrentEmail(currentUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
            .putExtra("sender", toUser.email)
        intent.action = "OPEN_CHAT_ACTIVITY"

        ActivityScenario.launch<LoginActivity>(intent).use {
            waitForLoading(it)

            // Assert that we launched the chat activity
            intended(allOf(
                hasComponent(ChatActivity::class.java.name),
                hasExtra("toUserEmail", toUser.email)
            ))
        }
    }

    @Test
    fun whenNonExistingUserLoggedInRedirectToSignupActivity() {
        // Make sure the database is empty before starting the test
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        val store = (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(nonExistingUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)

        ActivityScenario.launch<LoginActivity>(intent).use {
            waitForLoading(it)
            // Assert that we launched the signup activity
            intended(hasComponent(SignupActivity::class.java.name))
        }
    }

    // Waits for the activity to finish loading any async state
    private fun waitForLoading(scenario: ActivityScenario<LoginActivity>) {
        // Instead, make the test wait for the future to finish, and crash after a certain time
        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity {
            stateLoading = it.stateLoading
        }
        stateLoading.get(1000, TimeUnit.MILLISECONDS)
    }
}