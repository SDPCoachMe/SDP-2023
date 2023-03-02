package com.github.sdpcoachme

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoredActivityTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun boredActivityContainsAllElements() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), BoredActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        onView(withId(R.id.response)).check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.request)).check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.db)).check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.delete)).check(matches(ViewMatchers.isDisplayed()))

        scenario.close()
    }

//    @Test
//    fun fakeApiCalls() {
//        val intent = Intent(ApplicationProvider.getApplicationContext(), BoredActivity::class.java)
//        val scenario = ActivityScenario.launch<MainActivity>(intent)
//
//        onView(withId(R.id.delete)).perform(click())
//        onView(withId(R.id.response)).check(matches(withText("DB deleted")))
//
//        scenario.close()
//    }
}