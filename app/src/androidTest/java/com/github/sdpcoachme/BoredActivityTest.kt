package com.github.sdpcoachme

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoredActivityTest {

    //Create a fake webserver
//    private val mockWebServer = MockWebServer()

    //Before tag used to start the webserver before running all the tests
    @Before
    fun setup() {
//        mockWebServer.start(8080)
    }

    @Test
    fun boredActivityContainsAllElements() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), BoredActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Espresso.onView(withId(R.id.response)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.request)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.db)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.delete)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        scenario.close()
    }

    @Test
    fun fakeApiCalls() {

    }

    //Shutting the server done once all the tests are run
    @After
    fun teardown() {
//        mockWebServer.shutdown()
    }
}