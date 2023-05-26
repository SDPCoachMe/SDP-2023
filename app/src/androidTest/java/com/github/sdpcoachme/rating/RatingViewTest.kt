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

    private fun ratingViewWorksOn(
        title: String,
        initialRating: Int,
        onCancel: () -> Unit,
        onSubmit: (Int) -> Unit
    ) {
        composeTestRule.setContent {
            RatingView(
                title = title,
                initialRating = initialRating,
                onCancel = onCancel,
                onSubmit = onSubmit
            )
        }
        composeTestRule.onNodeWithTag(TITLE).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(DONE).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(CANCEL).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag(RATING_BAR).assertExists().assertIsDisplayed()
        for (i in 1..5) {
            composeTestRule.onNodeWithTag(RATING_STAR + i).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun ratingViewWorksWithInitialRating() {
        ratingViewWorksOn(
            title = "Rate this",
            initialRating = 3,
            onCancel = {},
            onSubmit = {}
        )
    }

    @Test
    fun ratingViewReturnsOnDoneClick() {
        var isDone = false

        composeTestRule.setContent {
            RatingView(
                title = "title",
                initialRating = 3,
                onCancel = {},
                onSubmit = {
                    isDone = true
                }
            )
        }
        assertThat(isDone, `is`(true))
    }

    @Test
    fun ratingViewReturnsOnCancelClick() {
        var isCancelled = false

        composeTestRule.setContent {
            RatingView(
                title = "title",
                initialRating = 3,
                onSubmit = {},
                onCancel = {
                    isCancelled = true
                }
            )
        }
        composeTestRule.onNodeWithTag(DONE).performClick()
        assertThat(isCancelled, `is`(true))
    }


}