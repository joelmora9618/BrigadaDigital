package com.jem.brigadadigital.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jem.brigadadigital.MainActivity
import com.jem.brigadadigital.R

class MyFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received: $token")
        // In a real app, send this token to Firestore so the server can push directly to this device
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d("FCM", "Message Received from: ${message.from}")

        // For data messages
        if (message.data.isNotEmpty()) {
            val title = message.data["titulo"] ?: "Emergencia"
            val body = message.data["descripcion"] ?: "Nuevo incidente activo"
            showEmergencyNotification(title, body)
        } 
        
        // For notification payloads
        message.notification?.let {
            val title = it.title ?: "Emergencia"
            val body = it.body ?: "Nuevo incidente activo"
            showEmergencyNotification(title, body)
        }
    }

    private fun showEmergencyNotification(title: String, body: String) {
        val channelId = "emergency_channel_id"
        val channelName = "Alertas de Emergencia"

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Optional: put extras if you want to deep link to the emergency
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom Sound setup
        // Note to developer: Place your custom siren sound (sirena.mp3) in res/raw/ folder.
        // For now, it will use the default ringtone if the file doesn't exist.
        // val soundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sirena)
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Replace with your actual app icon if needed
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri) // or `soundUri` when you have the file
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // Required for High Priority disruption
            // Example of Quick Actions:
            .addAction(0, "VOY", pendingIntent)
            .addAction(0, "NO VOY", pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo (API 26), a channel is required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones críticas de bomberos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                
                // If using a custom sound, uncomment these lines:
                /*
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(soundUri, audioAttributes)
                */
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1001, notificationBuilder.build())
    }
}
