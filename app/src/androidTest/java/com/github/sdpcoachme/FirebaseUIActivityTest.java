package com.github.sdpcoachme;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

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
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
                LAUNCH_TIMEOUT);
    }

    // TODO: can sign-in be done???
    /*@Test
    public void signIntoGoogleAccountResultsInCorrectMessage() {
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        // Click the delete account button
        ViewInteraction signInButton = onView(withId(R.id.sign_in));
        signInButton.perform(click());

        // Check that the delete confirmation dialog is displayed
        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.textContains("Signed in as: ")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), containsString("Signed in as: "));
        assertThat(confirmDialog.getText(), containsString("@"));
    }*/

    @Test
    public void signOutOfGoogleAccountResultsInCorrectMessage() {
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        // Click the delete account button
        ViewInteraction signOutButton = onView(withId(R.id.sign_out_button));
        signOutButton.perform(click());

        // Check that the delete confirmation dialog is displayed
        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.text("Signed out")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Signed out"));
    }

    @Test
    public void deleteGoogleAccountResultsInCorrectMessage() {

//        String expected = String.valueOf(R.string.deleted_accout);
        device.findObject(By.res("com.github.sdpcoachme:id/sign_in_button")).click();

        // Click the delete account button
        ViewInteraction deleteButton = onView(withId(R.id.delete_google_account));
        deleteButton.perform(click());

        // Check that the delete confirmation dialog is displayed
        UiObject2 confirmDialog = device.wait(
                Until.findObject(By.text("Account deleted")), 5000);
        assertNotNull(confirmDialog);

        assertThat(confirmDialog.getText(), is("Account deleted"));
    }

}
