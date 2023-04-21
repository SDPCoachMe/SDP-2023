package com.github.sdpcoachme.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.github.sdpcoachme.LoginActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    // [START receive_message]

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            // Get the notification title and body from the remote message.
            val notificationTitle = remoteMessage.notification!!.title
            val notificationBody = remoteMessage.notification!!.body

            // Create and send a customized notification.
            sendNotification("Custom Title", "Custom Body")
        }
    }


    /*override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        println(TAG + "From: " + remoteMessage.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            println(TAG + "Message data payload: " + remoteMessage.data)
            if ( *//* Check if data needs to be processed by long running job *//*true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob()
            } else {
                // Handle message within 10 seconds
                handleNow()
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            println(
                TAG + "Message Notification Body: " + remoteMessage.notification!!
                    .body
            )

            // Create and send a notification.
            sendNotification("The message body has been changed!!")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }*/
    // [END receive_message]
    // [START on_new_token]
    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    override fun onNewToken(token: String) {
        println(TAG + "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    // [END on_new_token]
    private fun scheduleJob() {
        // [START dispatch_job]
//        val work: OneTimeWorkRequest = Builder(MyWorker::class.java)
//            .build()
//        WorkManager.getInstance(this).beginWith(work).enqueue()
        // [END dispatch_job]
    }

    private fun handleNow() {
        println(TAG + "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
    }

    private fun sendNotificationPrevious(messageBody: String) {
        println("sendNotification() called with: messageBody = [$messageBody]")
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0  /*Request code*/ , intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FCM Message")
            .setContentText(messageBody)
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
        notificationManager.notify(0  /*ID of notification*/ , notificationBuilder.build())
    }

    private fun sendNotification(notificationTitle: String, notificationBody: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(notificationTitle) // Set the title of the notification
            .setContentText(notificationBody) // Set the body of the notification
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


//    class MyWorker(context: Context, workerParams: WorkerParameters) :
//        com.sun.tools.javac.api.JavacTaskPool.Worker(context, workerParams) {
//        fun doWork(): Result {
//            // TODO(developer): add long running task here.
//            return Result.success()
//        }
//    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}