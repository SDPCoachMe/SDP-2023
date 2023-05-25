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
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.AddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.messaging.ChatActivity
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
open class CoachesListActivityTest {

    private val defaultEmail = "example@email.com"

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var store: CachingStore

    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)

    lateinit var scenario: ActivityScenario<CoachesListActivity>

    // With this, tests will wait until activity has finished loading state
    @Before
    open fun setup() {
        // Refresh the CachingStore before each test
        ApplicationProvider.getApplicationContext<CoachMeTestApplication>().clearDataStoreAndResetCachingStore()
        store = (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).store
        store.retrieveData.get(1, SECONDS)
        store.setCurrentEmail(defaultEmail).get(100, MILLISECONDS)

        // Given nondeterministic behavior depending on order of tests, we reset the database here
        // TODO: this is temporary, we should find a better way to guarantee the database is refreshed
        //  before each test
        //database.restoreDefaultAccountsSetup()

        // Populate the database, and wait for it to finish
        populateDatabase(store).join()
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
            stateLoading = it.stateUpdated
        }
        stateLoading.get(1000, MILLISECONDS)
        Intents.init()
    }

    // Necessary since we don't do scenario.use { ... } in each test, which closes automatically
    @After
    open fun cleanup() {
        scenario.close()
        Intents.release()
    }

    @Test
    fun allCoachesExists() {
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
    fun whenClickingOnOwnCoachProfileActivityShowsOwnProfile() {
        val coach = COACH_1
        store.setCurrentEmail(coach.email).thenApply {
            // Click on own element
            composeTestRule.onNodeWithText(coach.address.name).assertIsDisplayed()
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
                .assertIsDisplayed()
                .performClick()

            // Check that the ProfileActivity is launched with the correct extras
            Intents.intended(allOf(
                hasComponent(ProfileActivity::class.java.name),
                hasExtra("email", coach.email),
                hasExtra("isViewingCoach", false)
            ))
        }
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

    // Subclass added to be able to run a different setup method (to simulate viewing contacts)
    class ContactsListTest: CoachesListActivityTest() {
        lateinit var store: CachingStore

        @Before
        override fun setup() {
            store = (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).store
            // Launch the activity
            populateDatabase(store).join()

            val contactIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
            contactIntent.putExtra("isViewingContacts", true)
            scenario = ActivityScenario.launch(contactIntent)

            lateinit var stateLoading: CompletableFuture<Void>
            scenario.onActivity {
                stateLoading = it.stateUpdated
            }
            stateLoading.get(1000, MILLISECONDS)
            Intents.init()
        }

        @Test
        fun whenViewingContactsAndClickingOnClientChatActivityIsLaunched() {
            val toEmail = "to@email.com"
            val coach = UserInfo(
                "Jane",
                "Doe",
                toEmail,
                "0987654321",
                LAUSANNE,
                false,
                emptyList(),
                emptyList()
            )
            composeTestRule.onNodeWithText(coach.address.name).assertIsDisplayed()
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
                .assertIsDisplayed()
                .performClick()

            // Check that the ChatActivity is launched with the correct extras
            Intents.intended(allOf(
                hasComponent(ChatActivity::class.java.name),
                hasExtra("toUserEmail", coach.email),
            ))
        }

        @Test
        fun dashboardHasRightTitleOnContactsList() {
            val title = (InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as CoachMeApplication).getString(R.string.chats)
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }

        @Test
        fun dashboardIsAccessibleAndDisplayableFromContactsList() {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }

        @Test
        fun filteringButtonIsNotShownInContactsList() {
            composeTestRule.onNodeWithTag(FILTER).assertDoesNotExist()
        }

    }

    companion object {
        fun populateDatabase(store: CachingStore): CompletableFuture<Void> {

            val emailFuture = store.setCurrentEmail("example@email.com")
            // Add a few coaches to the database
            val futures1 = COACHES.map { store.updateUser(it) }

            // Add non-coach user to the database
            val futures2 = NON_COACHES.map { store.updateUser(it) }

            return CompletableFuture.allOf(*futures1.toTypedArray(), *futures2.toTypedArray(), emailFuture)
        }
    }
}