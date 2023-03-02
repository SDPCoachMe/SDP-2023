package com.github.sdpcoachme;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.sdpcoachme.firebase.auth.FirebaseUIActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> testRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testButton(){
        String str = "Enter your name";
        Intents.init();

        onView(withId(R.id.mainName)).check(matches(withText(str)));
        onView(withId(R.id.mainButton)).check(matches(isDisplayed()));
        onView(withId(R.id.mainButton)).perform(click());

        Intents.intended(hasComponent(GreetingActivity.class.getName()));
        Intents.intended(hasExtra("name", str));

        Intents.release();
    }

    @Test
    public void goToSignInButtonTakesUserToFirebaseUIActivity(){
        Intents.init();

        onView(withId(R.id.go_to_sign_in_button)).check(matches(isDisplayed()));
        onView(withId(R.id.go_to_sign_in_button)).perform(click());

        Intents.intended(hasComponent(FirebaseUIActivity.class.getName()));

        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_out_button)).check(matches(isDisplayed()));
        onView(withId(R.id.delete_account_button)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_info)).check(matches(isDisplayed()));

        Intents.release();
    }
}