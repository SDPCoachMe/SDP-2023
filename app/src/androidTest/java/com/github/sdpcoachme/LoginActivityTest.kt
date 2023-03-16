package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase
import org.hamcrest.CoreMatchers
import org.hamcrest.core.AllOf.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class LoginActivityTest {
    private val launchTimeout = 5000L
    private lateinit var device: UiDevice
    private lateinit var signInButtonText: String
    private lateinit var signOutButtonText: String
    private lateinit var signedOutInfoText: String
    private lateinit var deleteButtonText: String
    private lateinit var deleteInfoText: String

    @Before
    fun startLoginActivityFromHomeScreen() {
        FirebaseAuth.getInstance().signOut()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        device = UiDevice.getInstance(instrumentation)

        device.pressHome()
        val launcherPackage: String = device.launcherPackageName
        ViewMatchers.assertThat(launcherPackage, CoreMatchers.notNullValue())

        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            launchTimeout
        )

        val intent = targetContext.packageManager
            .getLaunchIntentForPackage(targetContext.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        targetContext.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(targetContext.packageName).depth(0)),
            launchTimeout
        )

        // Get the strings from the resources
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deleteButtonText = context.resources.getString(
            context.resources.getIdentifier(
                "delete_account_button_text",
                "string",
                context.packageName
            )
        )

        deleteInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "account_deleted",
                "string",
                context.packageName
            )
        )

        signOutButtonText = context.resources.getString(
            context.resources.getIdentifier(
                "sign_out_button_text",
                "string",
                context.packageName
            )
        )

        signedOutInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "signed_out",
                "string",
                context.packageName
            )
        )

        signInButtonText = context.resources.getString(
            context.resources.getIdentifier(
                "sign_in_button_text",
                "string",
                context.packageName
            )
        )
    }

    @Test
    fun signOutOfGoogleAccountResultsInCorrectMessage() {
        val signOutButton = device.findObject(UiSelector().text(signOutButtonText))
        signOutButton.click()

        val confirmDialog: UiObject2 = device.wait(
            Until.findObject(By.text(signedOutInfoText)), 5000
        )

        TestCase.assertNotNull(confirmDialog)
        ViewMatchers.assertThat(confirmDialog.text, CoreMatchers.`is`(signedOutInfoText))
    }

    @Test
    fun deleteGoogleAccountResultsInCorrectMessage() {
        val deleteButton = device.findObject(UiSelector().text(deleteButtonText))
        deleteButton.click()
        val confirmDialog: UiObject2 = device.wait(
            Until.findObject(By.text(deleteInfoText)), 5000
        )
        TestCase.assertNotNull(confirmDialog)
        ViewMatchers.assertThat(confirmDialog.text, CoreMatchers.`is`(deleteInfoText))
    }

    //TODO implement this test
    // @Test
    fun signInLaunchesDashboardOnSuccess() {
        Intents.init()
        val email = "patrick.sebastien@gmail.com"
        // perform here a successful login with the email
        intended(allOf(
            hasComponent(DashboardActivity::class.java.name),
            hasExtra("signInInfo", email)))
        Intents.release()
    }
}