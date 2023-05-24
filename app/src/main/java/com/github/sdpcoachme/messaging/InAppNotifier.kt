package com.github.sdpcoachme.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.profile.CoachesListActivity
import com.google.firebase.messaging.FirebaseMessagingService
import java.util.concurrent.CompletableFuture

/**
 * This class is used to send in-app push notifications to the user while the app is in the foreground.
 * Whenever a push notification is received when in the foreground, this class is called from the
 * InAppNotificationService class to send the adapted notification (as we know more about the app's state
 * when the app is open).
 * @see InAppNotificationService
 *
 * @param context Context of the application
 * @param store CachingStore of the application
 */
class InAppNotifier(val context: Context, val store: CachingStore) {
    private val channelId = "fcm_default_channel"

    /**
     * Sends a push notification with the supplied arguments as parameters.
     *
     * @param title Title of the notification
     * @param body Body of the notification
     * @param chatId Id of the chat
     * @param notificationType Type of the notification
     */
    fun sendNotification(title: String?, body: String?, chatId: String?, notificationType: String?) {
        val notificationTitle = title?: "New message"
        val notificationBody = body?: "You have a new message"
        val id = chatId ?: ""

        // to enable multiple notification types, we check the notificationType field
        val type = notificationType ?: ""

        // Create and send a customized notification.
        if (type == "messaging") {
            sendMessagingNotification(notificationTitle, notificationBody, id)
        }
    }

    /**
     * Create and show a notification containing the received FCM message for in-app push notifications.
     * Depending on the current state of the application, the notification will open the chat activity,
     * the coaches list activity or the login activity (based on current user's email and the sender argument).
     *
     * @param notificationTitle Title of the notification
     * @param notificationBody Body of the notification
     * @param chatId Id of the chat
     */
    private fun sendMessagingNotification(notificationTitle: String, notificationBody: String, chatId: String) {

        // The more info we receive, the more we can customize the notification's behaviour (up until the chat itself)
        store.isLoggedIn().thenCompose { isLoggedIn ->
            if (isLoggedIn) store.getCurrentEmail() else CompletableFuture.completedFuture("")
        }.thenAccept { email ->
            val intent = when {
                email.isEmpty() ->
                    Intent(context, LoginActivity::class.java)
                        .putExtra("chatId", chatId)
                        .setAction("OPEN_CHAT_ACTIVITY")
                chatId.isEmpty() ->
                    Intent(context, CoachesListActivity::class.java)
                        .putExtra("isViewingContacts", true)
                        .putExtra("pushNotification_currentUserEmail", email)
                else ->
                    Intent(context, CoachesListActivity::class.java)
                        // openChat is added to make sure that the user goes bac to the CoachesListActivity
                        // when clicking on the back button in the chat activity
                        // I.e., with this boolean extra, the onCreate of the CoachesListActivity
                        // will automatically redirect to the wanted chat.
                        .putExtra("openChat", true)
                        .putExtra("chatId", chatId)
                        .putExtra("pushNotification_currentUserEmail", email)
            }

            // Create the pending intent to be used when the notification is clicked
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0 /* Request code */,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val (notificationBuilder, notificationManager, channel) =
                createNotificationElements(notificationTitle, notificationBody, pendingIntent)

            notificationManager.createNotificationChannel(channel)
            // current time is used to make sure that each notification id is unique
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }

    /**
     * Creates the notification elements.
     *
     * @param notificationTitle Title of the notification
     * @param notificationBody Body of the notification
     * @param pendingIntent Intent to be used when the notification is clicked
     */
    private fun createNotificationElements(
        notificationTitle: String,
        notificationBody: String,
        pendingIntent: PendingIntent?
    ): Triple<NotificationCompat.Builder, NotificationManager, NotificationChannel> {

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationTitle) // Set the title of the notification
            .setContentText(notificationBody) // Set the body of the notification
            .setSmallIcon(context.applicationInfo.icon)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager =
            context.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        return Triple(notificationBuilder, notificationManager, channel)
    }
}