package com.github.sdpcoachme.firebase.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.github.sdpcoachme.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FirebaseUIActivityTest {

    private static final String BASIC_SAMPLE_PACKAGE
            = "com.github.sdpcoachme";
    private static final int LAUNCH_TIMEOUT = 5000;
    private UiDevice device;

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressHome();

        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
                LAUNCH_TIMEOUT);
    }


    @Test
    public void signOutOfGoogleAccountResultsInCorrectMessage() {
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        ViewInteraction signOutButton = onView(withId(R.id.sign_out_button));
        signOutButton.perform(click());

        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.text("Signed out")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Signed out"));
    }

    @Test
    public void deleteGoogleAccountResultsInCorrectMessage() {

        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        ViewInteraction deleteButton = onView(withId(R.id.delete_google_account));
        deleteButton.perform(click());

        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.text("Account deleted")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Account deleted"));
    }


    // The following tests have been commented out because they only worked on the local emulator:
    /*@Test
    public void signIntoGoogleAccountResultsInFailedMessageIfNoAccountChosenAfterSignOut() {
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        ViewInteraction signOutButton = onView(withId(R.id.sign_out_button));
        signOutButton.perform(click());
        signOutButton.perform(click());

        ViewInteraction signInButton = onView(ViewMatchers.withId(R.id.sign_in));
        signInButton.perform(click());

        device.wait(Until.findObject(By.textContains("email")), 5000);

        device.pressBack();

        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.textContains("Sign in failed")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Sign in failed"));
    }
*/
    /*@Test
    public void signIntoGoogleAccountResultsInFailedMessageIfNoAccountChosenAfterDelete() {
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        ViewInteraction deleteButton = onView(withId(R.id.delete_google_account));
        deleteButton.perform(click());
        deleteButton.perform(click());

        ViewInteraction signInButton = onView(ViewMatchers.withId(R.id.sign_in));
        signInButton.perform(click());

        device.wait(Until.findObject(By.textContains("email")), 5000);

        device.pressBack();

        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.textContains("Sign in failed")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Sign in failed"));
    }*/
}
