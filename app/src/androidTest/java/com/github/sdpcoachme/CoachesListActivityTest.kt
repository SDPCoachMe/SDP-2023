package com.github.sdpcoachme

import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture

@RunWith(AndroidJUnit4::class)
class CoachesListActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun allCoachesExists() {
        // Populate the database
        populateDatabase().thenRun {
            // Launch the activity
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                // Check that all coaches are displayed

                // TODO: this is temporary ! We need to find a better way to wait for activities to fetch from the database
                SystemClock.sleep(500)
                coaches.forEach { coach ->
                    composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertExists()
                    composeTestRule.onNodeWithText(coach.location).assertExists()
                }
            }
        }
    }

    @Test
    fun allNonCoachesDoNotExist() {
        // Populate the database
        populateDatabase().thenRun {
            // Launch the activity
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), CoachesListActivity::class.java)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                // Check that all non coach users are not displayed

                SystemClock.sleep(500)
                coaches.forEach { coach ->
                    composeTestRule.onNodeWithText("${coach.firstName} ${coach.lastName}").assertDoesNotExist()
                    composeTestRule.onNodeWithText(coach.location).assertDoesNotExist()
                }
            }
        }
    }

    // TODO: add tests to see whether clicking on a coach opens the correct activity

    private val coaches = listOf(
        UserInfo(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@email.com",
            location = "Paris",
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = true
        ),
        UserInfo(
            firstName = "Marc",
            lastName = "Delémont",
            email = "marc@email.com",
            location = "Lausanne",
            phone = "0123456789",
            sports = listOf(Sports.WORKOUT),
            coach = true
        ),
        UserInfo(
            firstName = "Kate",
            lastName = "Senior",
            email = "katy@email.com",
            location = "Payerne",
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
            location = "Londres",
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = false
        ),
        UserInfo(
            firstName = "Loris",
            lastName = "Gotti",
            email = "lolo@email.com",
            location = "Corcelles",
            phone = "0123456789",
            sports = listOf(Sports.TENNIS),
            coach = false
        )
    )

    private fun populateDatabase(): CompletableFuture<Void> {
        val database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database

        // Add a few coaches to the database
        val futures1 = coaches.map { database.addUser(it) }

        // Add non-coach user to the database
        val futures2 = nonCoaches.map { database.addUser(it) }

        return CompletableFuture.allOf(*futures1.toTypedArray(), *futures2.toTypedArray())
    }

}