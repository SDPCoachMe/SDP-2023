package com.github.sdpcoachme.messaging

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
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
class MyFirebaseMessagingService : FirebaseMessagingService() {
    var notificationId = 0

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val notificationTitle = remoteMessage.notification!!.title?: "New message received"
            val notificationBody = remoteMessage.notification!!.body?: ""
            val sender = remoteMessage.data["sender"] ?: ""

            // to enable multiple notification types, we check the notificationType field
            val notificationType = remoteMessage.data["notificationType"] ?: ""

            // Create and send a customized notification.
            if (notificationType == "messaging") {
                sendMessagingNotification(notificationTitle, notificationBody, sender)
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message for in-app push notifications.
     *
     * @param notificationTitle Title of the notification
     * @param notificationBody Body of the notification
     */
    private fun sendMessagingNotification(notificationTitle: String, notificationBody: String, sender: String) {

        val email = (application as CoachMeApplication).database.getCurrentEmail()

        // TODO: at the moment, if the user is still in the login activity, the notification will cause an error (no email yet).
        //       Therefore, we are checking if the email is empty and if so, we send the user to the login activity.
        //       Once the storing of the email offline is done, this will work and the if check can be removed

        // The more info we receive, the more we can customize the notification's behaviour (up until the chat itself)
        val intent = Intent(
            this,
            if (email.isEmpty()) LoginActivity::class.java
            else if (sender.isEmpty()) CoachesListActivity::class.java
            else ChatActivity::class.java
        )

        if (sender.isNotEmpty()) {
            intent.putExtra("toUserEmail", sender)
        } else {
            intent.putExtra("isViewingContacts", true)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(notificationTitle) // Set the title of the notification
            .setContentText(notificationBody) // Set the body of the notification
            .setSmallIcon(application.applicationInfo.icon)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(notificationId, notificationBuilder.build())
        notificationId++
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