package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.LoginActivity.TestTags.Buttons.Companion.DELETE_ACCOUNT
import com.github.sdpcoachme.LoginActivity.TestTags.Buttons.Companion.SIGN_OUT
import com.github.sdpcoachme.LoginActivity.TestTags.Companion.INFO_TEXT
import com.github.sdpcoachme.map.MapActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Since those tests use UI Automator, we need to enable testTagsAsResourceId in the corresponding activity
// in order to be able to find the UI elements by their tags.
@RunWith(AndroidJUnit4::class)
open class LoginActivityTest {
    private val launchTimeout = 5000L
    private lateinit var device: UiDevice
    private lateinit var signedOutInfoText: String
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

        deleteInfoText = context.resources.getString(
            context.resources.getIdentifier(
                "account_deleted",
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
    }

    @Test
    fun signOutOfGoogleAccountResultsInCorrectMessage() {
        val signOutButton = device.findObject(By.res(SIGN_OUT))
        signOutButton.click()

        ViewMatchers.assertThat(device.findObject(By.res(INFO_TEXT)).text, CoreMatchers.`is`(signedOutInfoText))
    }

    @Test
    fun deleteGoogleAccountResultsInCorrectMessage() {
        val deleteButton = device.findObject(By.res(DELETE_ACCOUNT))
        deleteButton.click()

        ViewMatchers.assertThat(device.findObject(By.res(INFO_TEXT)).text, CoreMatchers.`is`(deleteInfoText))
    }

    //TODO implement this test
    // @Test
    fun signInLaunchesMapOnSuccess() {
        Intents.init()
        //val email = "patrick.sebastien@gmail.com"
        // perform here a successful login with the email
        intended(hasComponent(MapActivity::class.java.name))
        Intents.release()
    }
}