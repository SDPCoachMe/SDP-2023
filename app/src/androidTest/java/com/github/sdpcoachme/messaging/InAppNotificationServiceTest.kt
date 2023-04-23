package com.github.sdpcoachme.messaging

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.firebase.database.MockDatabase
import com.github.sdpcoachme.location.UserLocationSamples
import com.github.sdpcoachme.map.MapActivity
import com.google.firebase.messaging.RemoteMessage
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture

@RunWith(AndroidJUnit4::class)
class InAppNotificationServiceTest {

    @Test
    fun addFcmTokenToDatabasePlacesTokenIntoTheDb() {
        val expectedEmail = "test@email.com"
        var receivedEmail = ""
        var receivedToken = ""
        val future = CompletableFuture<Void>()
        class TestDB : MockDatabase() {
            override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
                receivedEmail = email
                receivedToken = token
                future.complete(null)
                return future
            }
        }
        val db = TestDB()
        db.setCurrentEmail(expectedEmail)

        InAppNotificationService.addFCMTokenToDatabase(db)

        val result = future.thenApply {
            assertThat(receivedEmail, `is`(expectedEmail))
            assertTrue(receivedToken.isNotEmpty()) // as the tokens depend on the device, we can't check for a specific value
            true
        }.exceptionally {
            false
        }.get(5, java.util.concurrent.TimeUnit.SECONDS) // added so we can check if the tests in the future succeed or not

        assertThat(result, `is`(true))
    }

    @Test
    fun checkInAppPushNotifications() {
        val notificationService = InAppNotificationService()

        val remoteMessage = RemoteMessage.Builder("test")
            .addData("title", "Title")
            .addData("body", "Body")
            .addData("sender", "sender@email.com")
            .addData("notificationType", "messaging")
            .build()

        println(remoteMessage.notification)
        println(remoteMessage.data["sender"])
        println(remoteMessage.data["notificationType"])
        println(remoteMessage.from)
//        notificationService.onMessageReceived()
    }
}