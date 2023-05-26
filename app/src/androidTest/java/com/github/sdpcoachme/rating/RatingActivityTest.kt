package com.github.sdpcoachme.rating

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.rating.RatingActivity.Companion.DEFAULT_COACH_NAME
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.DONE
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_BAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_STAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.TITLE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RatingActivityTest {

    private val launchRating = RatingActivity.getIntent(
        context = ApplicationProvider.getApplicationContext()
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule() //createAndroidComposeRule<SelectSportsActivity>()

    private fun assertStars(start: Int = 1, end: Int = 5, selected: Boolean = false) {
        for (i in start..end) {
            composeTestRule.onNodeWithTag(
                RATING_STAR + i.toString() + selected.toString(),
                useUnmergedTree = true
            ).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun defaultParametersAreDisplayedProperly() {
        ActivityScenario.launch<RatingActivity>(launchRating).use {
            composeTestRule.onNodeWithTag(TITLE).assertIsDisplayed().assertTextEquals("Rate $DEFAULT_COACH_NAME")
            composeTestRule.onNodeWithTag(DONE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CANCEL).assertIsDisplayed()
            composeTestRule.onNodeWithTag(RATING_BAR, useUnmergedTree = true).assertExists().assertIsDisplayed()
            assertStars()
        }
    }

    @Test
    fun intentParametersAreDisplayedProperly() {
        val coachName = "Peter Dinklage"
        val initialValue = 3
        val intent = RatingActivity.getIntent(
            context = ApplicationProvider.getApplicationContext(),
            coachName = coachName,
            initialValue = initialValue
        )
        ActivityScenario.launch<RatingActivity>(intent).use {

            composeTestRule.onNodeWithTag(TITLE)
                .assertIsDisplayed().assertTextEquals("Rate $coachName")
            assertStars(1, 3, true)
            assertStars(4, 5)
        }
    }

}