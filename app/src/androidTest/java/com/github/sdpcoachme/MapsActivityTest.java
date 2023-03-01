package com.github.sdpcoachme;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MapsActivityTest {
    @Test
    public void clickOnInfoWindowShowsToast() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapsActivity.class);
        ActivityScenario<GreetingActivity> scenario = ActivityScenario.launch(intent);
        ViewInteraction gMap = onView(withId(R.id.map));
        gMap.check(matches(isDisplayed()));
    }
}

