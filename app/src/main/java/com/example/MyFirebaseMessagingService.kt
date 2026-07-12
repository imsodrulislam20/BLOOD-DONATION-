package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.FirebaseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    private val CHANNEL_ID = "blood_donation_notifications"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        saveTokenToDatabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from FCM: ${remoteMessage.notification?.body}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "নতুন রক্তের আবেদন"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "জরুরি রক্তদান প্রয়োজন!"
        
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "রক্তদান নোটিফিকেশন",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "রক্তদান এবং যোগাযোগের নোটিফিকেশন চ্যানেল"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        fun saveTokenToDatabase(token: String) {
            if (!FirebaseManager.isInitialized || FirebaseManager.db == null) return
            val database = FirebaseManager.db ?: return
            val sanitizedToken = token.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("/", "_")
                .replace("[", "_")
                .replace("]", "_")

            val tokenData = mapOf(
                "token" to token,
                "platform" to "android",
                "createdAt" to System.currentTimeMillis(),
                "userId" to (FirebaseManager.auth?.currentUser?.uid ?: "anonymous")
            )

            database.getReference("fcmTokens").child(sanitizedToken).setValue(tokenData)
            
            FirebaseManager.auth?.currentUser?.uid?.let { uid ->
                database.getReference("users").child(uid).child("fcmTokens").child(sanitizedToken).setValue(tokenData)
            }
        }
    }
}
