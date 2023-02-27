package com.github.sdpcoachme;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

public class BoredActivityTest {
    @Test
    public void boredActivityContainsAllElements(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BoredActivity.class);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.response)).check(matches(isDisplayed()));
        onView(withId(R.id.request)).check(matches(isDisplayed()));
        onView(withId(R.id.db)).check(matches(isDisplayed()));
        onView(withId(R.id.delete)).check(matches(isDisplayed()));

        scenario.close();
    }
}
