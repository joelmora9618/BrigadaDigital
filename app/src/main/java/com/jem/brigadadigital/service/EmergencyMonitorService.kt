package com.jem.brigadadigital.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jem.brigadadigital.MainActivity
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class EmergencyMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository: EmergencyRepository = EmergencyRepositoryImpl()
    private var lastAlertTimestamp: Long = System.currentTimeMillis()
    private var isListening = false
    private var cuartelId: String? = null

    companion object {
        const val ACTION_START = "ACTION_START_MONITOR"
        const val ACTION_STOP = "ACTION_STOP_MONITOR"
        const val EXTRA_CUARTEL_ID = "EXTRA_CUARTEL_ID"
        const val MONITOR_NOTIFICATION_ID = 789
        const val ALERT_NOTIFICATION_ID = 1001
        const val CHANNEL_MONITOR = "monitor_channel"
        const val CHANNEL_ALERT = "emergency_channel_id"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newCuartelId = intent.getStringExtra(EXTRA_CUARTEL_ID)
                if (newCuartelId != null && newCuartelId != cuartelId) {
                    cuartelId = newCuartelId
                    startMonitoring(newCuartelId)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring(cuartelId: String) {
        if (isListening) return
        isListening = true

        createNotificationChannels()
        startForeground(MONITOR_NOTIFICATION_ID, createMonitorNotification())

        serviceScope.launch {
            repository.observeAllActiveEmergencies()
                .distinctUntilChanged()
                .collect { result ->
                    result.onSuccess { list ->
                        // Filtrar solo las de mi cuartel y que sean posteriores al inicio del servicio
                        val newAlerts = list.filter { 
                            it.cuartelId == cuartelId && it.timestamp > lastAlertTimestamp 
                        }

                        if (newAlerts.isNotEmpty()) {
                            newAlerts.forEach { alert ->
                                showEmergencyAlert(alert.titulo, "${alert.tipo}: ${alert.direccion}")
                                // Actualizamos el timestamp para no repetir esta alerta
                                if (alert.timestamp > lastAlertTimestamp) {
                                    lastAlertTimestamp = alert.timestamp
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun showEmergencyAlert(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createMonitorNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("Brigada Digital")
            .setContentText("Monitoreo de alertas activo")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel 1: Persistent Monitoring (Low importance)
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "Estado del Servicio",
                NotificationManager.IMPORTANCE_LOW
            )
            
            // Channel 2: Real Emergency Alerts (High importance / Sound)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Alertas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones críticas de siniestros"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
