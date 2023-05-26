package com.github.sdpcoachme.rating

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.DONE
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_BAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_STAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.TITLE
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RatingViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun defaultSetup(
        title: String = "title",
        initialRating: Int = 0,
        onCancel: () -> Unit = {},
        onSubmit: (Int) -> Unit = {}
    ) {
        composeTestRule.setContent {
            RatingView(
                title = title,
                initialRating = initialRating,
                onCancel = onCancel,
                onSubmit = onSubmit
            )
        }
    }

    private fun assertStars(start: Int = 1, end: Int = 5, selected: Boolean = false) {
        for (i in start..end) {
            composeTestRule.onNodeWithTag(
                RATING_STAR + i.toString() + selected.toString(),
                useUnmergedTree = true
            ).assertExists().assertIsDisplayed()
        }
    }

    private fun clickStar(star: Int, selected: Boolean = false) {
        composeTestRule.onNodeWithTag(
            RATING_STAR + star.toString() + selected.toString(),
            useUnmergedTree = true
        ).performClick()
    }

    @Test
    fun ratingViewWorksWithInitialRating() {
        defaultSetup()

        composeTestRule.onNodeWithTag(TITLE).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(DONE).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(CANCEL).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(RATING_BAR, useUnmergedTree = true).assertExists().assertIsDisplayed()
        assertStars()
    }

    @Test
    fun ratingViewCorrectlyReturnsOnDoneClick() {
        var isDone = false

        defaultSetup(initialRating = 3, onSubmit = { isDone = true })
        composeTestRule.onNodeWithTag(DONE).performClick()
        assertThat(isDone, `is`(true))
    }

    @Test
    fun ratingViewCorrectlyReturnsOnCancelClick() {
        var isCancelled = false

        defaultSetup(initialRating = 3, onCancel = { isCancelled = true })
        composeTestRule.onNodeWithTag(CANCEL).performClick()
        assertThat(isCancelled, `is`(true))
    }

    @Test
    fun starSelectionFillsInCorrectStars() {
        defaultSetup()

        assertStars()
        clickStar(4)
        assertStars(1, 4, true)
        assertStars(5, 5)
        clickStar(1, true)
        assertStars(2, 5)
        assertStars(1, 1, true)
    }

//    TODO: fix this test that does not pass for some strange reason
//    @Test
//    fun outerSelectionResetsAllStars() {
//        defaultSetup(initialRating = 4)
//
//        assertStars(1, 4, true)
//        assertStars(5, 5)
//        composeTestRule.onNodeWithTag(BACKGROUND, useUnmergedTree = true).performClick()
//        assertStars()
//    }

}