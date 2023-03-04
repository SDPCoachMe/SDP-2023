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
    private val LAUNCH_TIMEOUT = 5000L
    private lateinit var device: UiDevice
    private lateinit var signInButtonText: String
    private lateinit var signOutButtonText: String
    private lateinit var signedOutInfoText: String
    private lateinit var deleteButtonText: String
    private lateinit var deleteInfoText: String

    @Before
    fun startMainActivityFromHomeScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        device = UiDevice.getInstance(instrumentation)

        device.pressHome()
        val launcherPackage: String = device.launcherPackageName
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
        val goToSignInButton = device.findObject(UiSelector().text("Go to sign in page"))
        goToSignInButton.click()

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

        assertNotNull(confirmDialog)
        assertThat(confirmDialog.text, `is`(signedOutInfoText))
    }

    @Test
    fun deleteGoogleAccountResultsInCorrectMessage() {
        val deleteButton = device.findObject(UiSelector().text(deleteButtonText))
        deleteButton.click()
        val confirmDialog: UiObject2 = device.wait(
            Until.findObject(By.text(deleteInfoText)), 5000
        )
        assertNotNull(confirmDialog)
        assertThat(confirmDialog.text, `is`(deleteInfoText))
    }
}