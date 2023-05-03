package com.github.sdpcoachme.messaging

import android.annotation.SuppressLint
import android.content.ContentValues
import android.util.Log
import com.github.sdpcoachme.CoachMeApplication
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.CompletableFuture

/**
 * This service handles all incoming push notifications when the app is in the foreground.
 * Whenever the app is open and a push notification is received, this service will be called.
 * This is done to enable more specific push notification functionalities than the generic ones
 * from the cloud functions. This way, for instance, we can skip the login activity if the user
 * is already logged in. When the app is not in the foreground, however, this service will not be
 * called and the app will just open the login activity with the arguments from the push notification
 * sent from the cloud function.
 *
 * To enable testing, the actual notification is sent from the InAppNotifier class (as it is not
 * possible to create RemoteMessages containing notifications inside tests).
 * @see InAppNotifier
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh") // as we do not yet have the user's email at start up, we cannot add the token to the database then and overriding and implementing this method could cause an error.
class InAppNotificationService : FirebaseMessagingService() {

    /**
     * Called when a push notification is received while the app is in the foreground.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val notificationTitle = remoteMessage.notification
            val notificationBody = remoteMessage.notification
            val sender = remoteMessage.data["sender"]
            val notificationType = remoteMessage.data["notificationType"]

            // Since it does not seem to be possible to create RemoteMessages containing a notification,
            // the in-app push notification part has been moved to the InAppNotifier class to enable testing.
            InAppNotifier(this, (application as CoachMeApplication).store)
                .sendNotification(
                    title = notificationTitle!!.title,
                    body = notificationBody!!.body,
                    senderEmail = sender,
                    notificationType = notificationType
            )
        }
    }

    companion object {

        /**
         * Adds the FCM token of the current user to the database.
         */
        fun getFCMToken(): CompletableFuture<String> {
            val future = CompletableFuture<String>()
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                    future.completeExceptionally(task.exception)
                } else {
                    // Get new FCM registration token
                    val token = task.result
                    future.complete(token)
                }
            }
            return future
        }
    }
}