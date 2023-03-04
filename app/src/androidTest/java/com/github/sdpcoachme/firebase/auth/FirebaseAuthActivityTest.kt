package com.github.sdpcoachme.firebase.auth

import android.content.Intent
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Test


class FirebaseAuthActivityTest {
    private val BASIC_SAMPLE_PACKAGE = "com.github.sdpcoachme"
    private val LAUNCH_TIMEOUT = 5000L
    private lateinit var device: UiDevice

//    @Before
//    fun startMainActivityFromHomeScreen() {
//        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//        device.pressHome()
//        val launcherPackage: String = device.getLauncherPackageName()
//        assertThat(launcherPackage, notNullValue())
//        device.wait(
//            Until.hasObject(By.pkg(launcherPackage).depth(0)),
//            LAUNCH_TIMEOUT
//        )
//        val context: android.content.Context = ApplicationProvider.getApplicationContext()
//        val intent: Intent = context.getPackageManager()
//            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//        context.startActivity(intent)
//        device.wait(
//            Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
//            LAUNCH_TIMEOUT
//        )
//        device.findObject(By.res("com.github.sdpcoachme:id/go_to_sign_in_button")).click()
//    }
    @Before
    fun startMainActivityFromHomeScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        device = UiDevice.getInstance(instrumentation)

        device.pressHome()
        val launcherPackage: String = device.getLauncherPackageName()
        assertThat(launcherPackage, notNullValue())

        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT
        )

        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        targetContext.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(targetContext.packageName).depth(0)),
            LAUNCH_TIMEOUT
        )

//        device.findObject(By.res("com.github.sdpcoachme:id/signInPageButton")).click()
        val goToSignInButton = device.findObject(UiSelector().text("Go to sign in page"))
        goToSignInButton.click()
    }



    @Test
    fun signOutOfGoogleAccountResultsInCorrectMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // access sign_out_button_text from strings.xml without R.string...
        val signOutButtonText = context.resources.getString(
            context.resources.getIdentifier(
                "sign_out_button_text",
                "string",
                context.packageName
            )
        )

        val signedOutInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "signed_out",
                "string",
                context.packageName
            )
        )

        val signOutButton = device.findObject(UiSelector().text(signOutButtonText))
        signOutButton.click()

        val confirmDialog: UiObject2 = device.wait(
            Until.findObject(By.text(signedOutInfoText)), 5000
        )

        assertNotNull(confirmDialog)
        assertThat(confirmDialog.text, `is`(signedOutInfoText))
    }

    @Test
    fun deleteGoogleAccountResultsInCorrectMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val deleteButtonText = context.resources.getString(
            context.resources.getIdentifier(
                "delete_account_button_text",
                "string",
                context.packageName
            )
        )

        val deleteInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "account_deleted",
                "string",
                context.packageName
            )
        )

        val deleteButton = device.findObject(UiSelector().text(deleteButtonText))
        deleteButton.click()
        val confirmDialog: UiObject2 = device.wait(
            Until.findObject(By.text(deleteInfoText)), 5000
        )
        assertNotNull(confirmDialog)
        assertThat(confirmDialog.text, `is`(deleteInfoText))
    }

//    @Test
//    fun signInTest() {
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//
//        val deleteButtonText = context.resources.getString(
//            context.resources.getIdentifier(
//                "delete_account_button_text",
//                "string",
//                context.packageName
//            )
//        )
//
//        val signOutButtonText = context.resources.getString(
//            context.resources.getIdentifier(
//                "sign_out_button_text",
//                "string",
//                context.packageName
//            )
//        )
//
//        val deleteButton = device.findObject(UiSelector().text(deleteButtonText))
//        val signOutButton = device.findObject(UiSelector().text(signOutButtonText))
//        deleteButton.click()
//        signOutButton.click()
//
//        val signInButtonText = context.resources.getString(
//            context.resources.getIdentifier(
//                "sign_in_button_text",
//                "string",
//                context.packageName
//            )
//        )
//
//        val signInInfoText = context.resources.getString(
//            context.resources.getIdentifier(
//                "signed_in_as",
//                "string",
//                context.packageName
//            )
//        )
//
//        val signInButton = device.findObject(UiSelector().text(signInButtonText))
//        signInButton.click()
//
//        device.pressBack()
//
//        val confirmDialog: UiObject2 = device.wait(
//            Until.findObject(By.text(signInInfoText)), 5000
//        )
//        assertNotNull(confirmDialog)
//        assertThat(confirmDialog.text, `is`(signInInfoText))
//    }


    // The following tests have been commented out because they only worked on the local emulator:
    /*@Test
    public void signIntoGoogleAccountResultsInFailedMessageIfNoAccountChosenAfterSignOut() {
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