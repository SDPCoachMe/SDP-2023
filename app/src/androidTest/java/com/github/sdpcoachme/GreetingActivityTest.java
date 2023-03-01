package com.github.sdpcoachme;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GreetingActivityTest {

    @Test
    public void greetingText(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), GreetingActivity.class);
        String str = "Jeremy";
        intent.putExtra("name", str);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.message)).check(matches(isDisplayed()));
        onView(withId(R.id.message)).check(matches(withText("Jeremy")));

        scenario.close();
    }

    @Test
    public void buttonOpensMap() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), GreetingActivity.class);
        ActivityScenario<GreetingActivity> scenario = ActivityScenario.launch(intent);
        ViewInteraction button = onView(withId(R.id.open_maps_btn));
        button.check(matches(isDisplayed()));
        button.perform(click());

        onView(withId(R.id.map)).check(matches(isDisplayed()));

        scenario.close();
    }

}
