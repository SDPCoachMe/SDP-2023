package com.example.bootcamp;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;

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

}
