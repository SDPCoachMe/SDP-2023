package com.github.sdpcoachme.profile

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.profile.CoachesListActivity.TestTags.Buttons.Companion.FILTER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS

@RunWith(AndroidJUnit4::class)
open class CoachesListActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database as MockDatabase

    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)

    lateinit var scenario: ActivityScenario<CoachesListActivity>

    private val defaultEmail = "example@email.com"
    // With this, tests will wait until activity has finished loading state
    @Before
    fun setup() {
        // Given nondeterministic behavior depending on order of tests, we reset the database here
        // TODO: this is temporary, we should find a better way to guarantee the database is refreshed
        //  before each test
        database.restoreDefaultAccountsSetup()

        // Populate the database, and wait for it to finish
        populateDatabase(database, defaultEmail).join()
        scenario = ActivityScenario.launch(defaultIntent)

        // This is the proper way of waiting for an activity to finish loading. However, it does not
        // crash if the activity never finishes loading, so we do not use it.
        /*
        scenario.onActivity {
            composeTestRule.registerIdlingResource(
                object : IdlingResource {
                    override val isIdleNow: Boolean
                        get() = it.stateLoading.isDone
                }
            )
        }
        */
        // Instead, make the test wait for the future to finish, and crash after a certain time
        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity {
            stateLoading = it.stateLoading
        }
        stateLoading.get(1000, MILLISECONDS)
        Intents.init()
    }

    // Necessary since we don't do scenario.use { ... } in each test, which closes automatically
    @After
    fun cleanup() {
        scenario.close()
        Intents.release()
    }

    @Test
    fun allCoachesExists() {
        sleep(10000)
        COACHES.forEach { coach ->
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertIsDisplayed()
            composeTestRule.onNodeWithText(coach.address.name).assertIsDisplayed()
        }
    }

    @Test
    fun allNonCoachesDoNotExist() {
        NON_COACHES.forEach { coach ->
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertDoesNotExist()
            composeTestRule.onNodeWithText(coach.address.name).assertDoesNotExist()
        }
    }

    // TODO: add a test that checks that the coaches are sorted by distance, however it is hard to do
    //  and not a priority since it requires mocking the location service and some complex matcher
    //  logic

    @Test
    fun whenClickingOnACoachProfileActivityShowsCoachToClient() {
            // Click on the first coach
            val coach = COACH_1
            composeTestRule.onNodeWithText(coach.address.name).assertIsDisplayed()
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
                .assertIsDisplayed()
                .performClick()

            // Check that the ProfileActivity is launched with the correct extras
            Intents.intended(allOf(
                hasComponent(ProfileActivity::class.java.name),
                hasExtra("email", coach.email),
                hasExtra("isViewingCoach", true)
            ))
    }

    @Test
    fun dashboardHasRightTitleOnNearbyCoachesList() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.title_activity_coaches_list)
        ActivityScenario.launch<CoachesListActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }
    }
    @Test
    fun dashboardIsAccessibleAndDisplayableFromNearbyCoachesList() {
        ActivityScenario.launch<CoachesListActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun errorPageIsShownWhenCoachesListIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<CoachesListActivity>(defaultIntent).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun filteringButtonIsShownInNearbyCoaches() {
        composeTestRule.onNodeWithTag(FILTER).assertExists().assertIsDisplayed()
    }

    @Test
    fun filteringButtonLaunchesSelectSportsActivity() {
        composeTestRule.onNodeWithTag(FILTER).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(FILTER).performClick()
        Intents.intended(allOf(hasComponent(SelectSportsActivity::class.java.name),
            hasExtra("title", "Filter coaches by sport")))
    }

    companion object {
        fun populateDatabase(database: Database, defaultEmail: String): CompletableFuture<Void> {

            database.setCurrentEmail(defaultEmail)
            // Add a few coaches to the database
            val futures1 = COACHES.map { database.updateUser(it) }

            // Add non-coach user to the database
            val futures2 = NON_COACHES.map { database.updateUser(it) }

            return CompletableFuture.allOf(*futures1.toTypedArray(), *futures2.toTypedArray())
        }
    }
}