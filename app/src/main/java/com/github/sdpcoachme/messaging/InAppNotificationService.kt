package com.github.sdpcoachme.messaging

import android.annotation.SuppressLint
import android.content.ContentValues
import android.util.Log
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.firebase.database.Database
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * This service handles all incoming push notifications.
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh") // as we do not yet have the user's email at start up, we cannot add the token to the database then and overriding and implementing this method would cause an error.
class InAppNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val notificationTitle = remoteMessage.notification
            val notificationBody = remoteMessage.notification
            val sender = remoteMessage.data["sender"]
            val notificationType = remoteMessage.data["notificationType"]

            // Since does not seem to be possible to create RemoteMessages containing a notification,
            // the in-app push notification part has been moved to the InAppNotifier class to enable testing.
            InAppNotifier(this, (application as CoachMeApplication).database)
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
        fun addFCMTokenToDatabase(database: Database) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                database.setFCMToken(database.getCurrentEmail(), token)
            })
        }
    }
}