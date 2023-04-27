package com.github.sdpcoachme.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.profile.CoachesListActivity
import com.google.firebase.messaging.FirebaseMessagingService

/**
 * This class handles the incoming messages and sends a push notification.
 * It has been created to enable testing of the push notifications.
 *
 * @param context Context of the application
 * @param database Database of the application
 */
class InAppNotifier(val context: Context, val database: Database) {

    /**
     * Sends a push notification with the supplied arguments as parameters.
     *
     * @param title Title of the notification
     * @param body Body of the notification
     * @param senderEmail Email of the sender
     * @param notificationType Type of the notification
     */
    fun sendNotification(title: String?, body: String?, senderEmail: String?, notificationType: String?) {
        val notificationTitle = title?: "New message"
        val notificationBody = body?: "You have a new message"
        val sender = senderEmail ?: ""

        // to enable multiple notification types, we check the notificationType field
        val type = notificationType ?: ""

        // Create and send a customized notification.
        if (type == "messaging") {
            sendMessagingNotification(notificationTitle, notificationBody, sender)
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message for in-app push notifications.
     *
     * @param notificationTitle Title of the notification
     * @param notificationBody Body of the notification
     */
    private fun sendMessagingNotification(notificationTitle: String, notificationBody: String, sender: String) {

        // TODO: at the moment, if the user is still in the login activity, the notification will cause an error (no email yet).
        //       Therefore, we are checking if the email is empty and if so, we send the user to the login activity.
        //       Once the storing of the email offline is done, this will work and the if check can be removed

        // The more info we receive, the more we can customize the notification's behaviour (up until the chat itself)
        val intent: Intent
        when {
            database.getCurrentEmail().isEmpty() -> {
                intent = Intent(context, LoginActivity::class.java)
                    .putExtra("sender", sender)
                intent.action = "OPEN_CHAT_ACTIVITY"
            }
            sender.isEmpty() -> intent = Intent(context, CoachesListActivity::class.java)
                .putExtra("isViewingContacts", true)
            else -> intent = Intent(context, ChatActivity::class.java)
                .putExtra("toUserEmail", sender)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationTitle) // Set the title of the notification
            .setContentText(notificationBody) // Set the body of the notification
            .setSmallIcon(context.applicationInfo.icon)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = context.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}