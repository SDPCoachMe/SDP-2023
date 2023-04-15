package com.github.sdpcoachme

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
import com.github.sdpcoachme.location.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.location.UserLocationSamples.Companion.LONDON
import com.github.sdpcoachme.location.UserLocationSamples.Companion.PARIS
import com.github.sdpcoachme.location.UserLocationSamples.Companion.SYDNEY
import com.github.sdpcoachme.location.UserLocationSamples.Companion.TOKYO
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.schedule.ScheduleActivity
import com.github.sdpcoachme.messaging.ChatActivity
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CoachesListActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database

    lateinit var scenario: ActivityScenario<CoachesListActivity>

    // With this, tests will wait until activity has finished loading state
    @Before
    fun setup() {
        // Populate the database, and wait for it to finish
        populateDatabase().join()
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
        scenario = ActivityScenario.launch(scheduleIntent)

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
        scenario.onActivity {
            it.stateLoading.get(1000, TimeUnit.MILLISECONDS)
        }
    }

    // Necessary since we don't do scenario.use { ... } in each test, which closes automatically
    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun allCoachesExists() {
        coaches.forEach { coach ->
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertIsDisplayed()
            composeTestRule.onNodeWithText(coach.location.address).assertIsDisplayed()
        }
    }

    @Test
    fun allNonCoachesDoNotExist() {
        nonCoaches.forEach { coach ->
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertDoesNotExist()
            composeTestRule.onNodeWithText(coach.location.address).assertDoesNotExist()
        }
    }

    // TODO: add a test that checks that the coaches are sorted by distance, however it is hard to do
    //  and not a priority since it requires mocking the location service and some complex matcher
    //  logic

    @Test
    fun whenClickingOnACoachProfileActivityShowsCoachToClient() {
            Intents.init()

            // Click on the first coach
            val coach = coaches[0]
            composeTestRule.onNodeWithText(coach.location.address).assertIsDisplayed()
            composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
                .assertIsDisplayed()
                .performClick()

            // Check that the ProfileActivity is launched with the correct extras
            Intents.intended(allOf(
                hasComponent(ProfileActivity::class.java.name),
                hasExtra("email", coach.email),
                hasExtra("isViewingCoach", true)
            ))

            Intents.release()
    }

    @Test
    fun whenViewingContactsAndClickingOnClientChatActivityIsLaunched() {
        // Populate the database
        populateDatabase().thenRun {
            // Launch the activity
            val contactIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
            contactIntent.putExtra("isViewingContacts", true)
            ActivityScenario.launch<ScheduleActivity>(contactIntent).use {
                Intents.init()
                SystemClock.sleep(500)

                // TODO: refactor this smartly!!!
                val toEmail = "to@email.com"
                val coach = UserInfo(
                    "Jane",
                    "Doe",
                    toEmail,
                    "0987654321",
                    "Some location",
                    false,
                    emptyList(),
                    emptyList()
                )
                composeTestRule.onNodeWithText(coach.location).assertIsDisplayed()
                composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}")
                    .assertIsDisplayed()
                    .performClick()

                // Check that the ChatActivity is launched with the correct extras
                Intents.intended(allOf(
                    hasComponent(ChatActivity::class.java.name),
                    hasExtra("toUserEmail", coach.email),
                ))

                Intents.release()
            }
        }
            Intents.release()
    }

    private val coaches = listOf(
        UserInfo(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@email.com",
            location = PARIS,
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = true
        ),
        UserInfo(
            firstName = "Marc",
            lastName = "Delémont",
            email = "marc@email.com",
            location = LAUSANNE,
            phone = "0123456789",
            sports = listOf(Sports.WORKOUT),
            coach = true
        ),
        UserInfo(
            firstName = "Kate",
            lastName = "Senior",
            email = "katy@email.com",
            location = LONDON,
            phone = "0123456789",
            sports = listOf(Sports.TENNIS, Sports.SWIMMING),
            coach = true
        )
    )

    private val nonCoaches = listOf(
        UserInfo(
            firstName = "James",
            lastName = "Dolorian",
            email = "jammy@email.com",
            location = TOKYO,
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = false
        ),
        UserInfo(
            firstName = "Loris",
            lastName = "Gotti",
            email = "lolo@email.com",
            location = SYDNEY,
            phone = "0123456789",
            sports = listOf(Sports.TENNIS),
            coach = false
        )
    )

    private fun populateDatabase(): CompletableFuture<Void> {

        database.setCurrentEmail("example@email.com")
        // Add a few coaches to the database
        val futures1 = coaches.map { database.updateUser(it) }

        // Add non-coach user to the database
        val futures2 = nonCoaches.map { database.updateUser(it) }

        return CompletableFuture.allOf(*futures1.toTypedArray(), *futures2.toTypedArray())
    }
}