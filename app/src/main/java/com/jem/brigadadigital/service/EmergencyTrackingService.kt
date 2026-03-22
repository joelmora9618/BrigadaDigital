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
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import com.jem.brigadadigital.R
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EmergencyTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository: EmergencyRepository = EmergencyRepositoryImpl()
    
    private var isTracking = false
    private var uid: String? = null
    private var emergencyId: String? = null
    private var targetLat: Double? = null
    private var targetLon: Double? = null
    private var hasArrived = false

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val EXTRA_UID = "EXTRA_UID"
        const val EXTRA_EMERGENCY_ID = "EXTRA_EMERGENCY_ID"
        const val EXTRA_TARGET_LAT = "EXTRA_TARGET_LAT"
        const val EXTRA_TARGET_LON = "EXTRA_TARGET_LON"
        const val NOTIFICATION_ID = 456
        const val CHANNEL_ID = "tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    Log.d("TrackingService", "Ubicación obtenida: ${location.latitude}, ${location.longitude}")
                    
                    val safeUid = uid
                    val safeEmergency = emergencyId
                    if (safeUid != null && safeEmergency != null) {
                        serviceScope.launch {
                            repository.updateTrackerLocation(safeEmergency, safeUid, geoPoint)
                            
                            // Verificar Arribo
                            if (!hasArrived && targetLat != null && targetLon != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    location.latitude, location.longitude,
                                    targetLat!!, targetLon!!,
                                    results
                                )
                                val distance = results[0]
                                if (distance < 50) { // 50 metros
                                    hasArrived = true
                                    repository.markAsArrived(safeEmergency, safeUid)
                                    updateNotification("¡HAS LLEGADO AL LUGAR!", "Iniciando operaciones de respuesta.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification(title, text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                uid = intent.getStringExtra(EXTRA_UID)
                emergencyId = intent.getStringExtra(EXTRA_EMERGENCY_ID)
                if (intent.hasExtra(EXTRA_TARGET_LAT)) {
                    targetLat = intent.getDoubleExtra(EXTRA_TARGET_LAT, 0.0)
                    targetLon = intent.getDoubleExtra(EXTRA_TARGET_LON, 0.0)
                }
                startTracking()
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        startForeground(NOTIFICATION_ID, createNotification())

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("TrackingService", "Started location tracking")
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Location permission missing", e)
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(
        title: String = "Misión Activa",
        text: String = "Compartiendo tu ubicación con el cuartel..."
    ): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Emergencia",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, EmergencyTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "LLEGUÉ AL LUGAR", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
