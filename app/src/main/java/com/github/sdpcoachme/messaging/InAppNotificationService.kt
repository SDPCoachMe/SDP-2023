package com.github.sdpcoachme.messaging

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachesListActivity
import com.github.sdpcoachme.LoginActivity
import com.github.sdpcoachme.firebase.database.Database
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * This service handles all incoming push notifications.
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh") // as we do not yet have the user's email at start up, we cannot add the token to the database then and overriding this method would cause an error.
class InAppNotificationService : FirebaseMessagingService() {
    private val notifier = InAppNotifier(this, (application as CoachMeApplication).database)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val notificationTitle = remoteMessage.notification
            val notificationBody = remoteMessage.notification
            val sender = remoteMessage.data["sender"]
            val notificationType = remoteMessage.data["notificationType"]

            notifier.onMessageReceived(
                title = notificationTitle!!.title,
                body = notificationBody!!.body,
                senderEmail = sender,
                notificationType = notificationType
            )
        }
    }

    companion object {
        /**
         * Adds the FCM token to the database.
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