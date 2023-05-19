package com.github.sdpcoachme.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.AddressSamples.Companion.PARIS
import com.github.sdpcoachme.profile.SelectSportsActivity.*
import com.github.sdpcoachme.profile.SelectSportsActivity.Companion.DEFAULT_TITLE
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Buttons.*
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Buttons.Companion.DONE
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.MultiSelectListTag.Companion.ROW_TEXT_LIST
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
open class SelectSportsActivityTest {

    val email: String = "test@email.com"
    private val userInfo = UserInfo(
        firstName = "John",
        lastName = "Doe",
        email = email,
        address = PARIS,
        phone = "0123456789",
        coach = false
    )
    private val store = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).store


    private val launchSelectSports = SelectSportsActivity.getIntent(
        context = ApplicationProvider.getApplicationContext()
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule() //createAndroidComposeRule<SelectSportsActivity>()

    @Before
    fun setup() { // set user in db to default
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.updateUser(userInfo).join()
        store.setCurrentEmail(email)
    }

    @Test
    fun defaultParametersAreDisplayedProperly() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            composeTestRule.onNodeWithTag(TITLE).assertIsDisplayed().assertTextEquals(DEFAULT_TITLE)
            composeTestRule.onNodeWithTag(DONE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CANCEL).assertIsDisplayed()
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }

    @Test
    fun intentParametersAreDisplayedProperly() {
        val title = "Title"
        val initialValue = listOf(Sports.SKI, Sports.SWIMMING)
        val intent = SelectSportsActivity.getIntent(
            context = ApplicationProvider.getApplicationContext(),
            title = title,
            initialValue = initialValue
        )
        ActivityScenario.launch<EditTextActivity>(intent).use {
            for (sport in Sports.values()) {
                if (sport in initialValue) {
                    composeTestRule.onNodeWithTag(TestTags.ListRowTag(sport).ICON, useUnmergedTree = true).assertIsDisplayed()
                } else {
                    composeTestRule.onNodeWithTag(TestTags.ListRowTag(sport).ICON, useUnmergedTree = true).assertDoesNotExist()
                }
            }
            composeTestRule.onNodeWithTag(TITLE).assertIsDisplayed().assertTextEquals(title)
            composeTestRule.onNodeWithTag(DONE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CANCEL).assertIsDisplayed()
            for (sport in Sports.values()) {
                composeTestRule.onNodeWithTag(TestTags.ListRowTag(sport).ROW).assertIsDisplayed()
                composeTestRule.onNodeWithTag(TestTags.ListRowTag(sport).ROW).assertTextEquals(sport.sportName)
            }
        }
    }

    @Test
    fun tickIconsDisplayedAfterClick() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun tickIconsDisappearAfterSecondClick() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }
}