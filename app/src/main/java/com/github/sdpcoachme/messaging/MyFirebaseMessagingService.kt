package com.github.sdpcoachme.messaging

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
import com.github.sdpcoachme.data.messaging.FCMToken
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * This service handles all incoming push notifications.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            // Get the notification title and body from the remote message.

            // If we decide to use more push notifications other than messaging, we can adapt the following
            // to use the title of the notification to determine what to do.
            val notificationTitle = remoteMessage.notification!!.title?: "New message received"
            val notificationBody = remoteMessage.notification!!.body?: ""
            val sender = remoteMessage.data["sender"] ?: ""

            val notificationType = remoteMessage.data["notificationType"] ?: ""

            // Create and send a customized notification.
            if (notificationType == "messaging") {
                sendMessagingNotification(notificationTitle, notificationBody, sender)
            }
        }
    }

    /**
     * There are two scenarios when onNewToken is called:
     *
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed:
     *      a) App is restored to a new device
     *      b) User uninstalls / reinstalls the app
     *      c) User clears app data
     */
    override fun onNewToken(token: String) {
        // We always add the token to the database, so that if the user enables notifications,
        // the token is already in the database and notifications can be received right away.
        addFCMTokenToDatabase()
    }

    /**
     * Adds the FCM token to the database.
     */
    private fun addFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            val database = (application as CoachMeApplication).database
            database.setFCMToken(database.getCurrentEmail(), FCMToken(token!!, true))
        })
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
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}