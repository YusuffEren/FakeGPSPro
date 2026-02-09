package com.yusuferen.mockgps.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yusuferen.mockgps.ui.models.LatLng
import com.yusuferen.mockgps.FakeGPSProApp
import com.yusuferen.mockgps.MainActivity
import com.yusuferen.mockgps.R
import com.yusuferen.mockgps.storage.StorageManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MockLocationService : Service() {

    companion object {
        const val TAG = "MockLocationService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mock_location_channel"
        var instance: MockLocationService? = null
    }

    var isMocking = false
        private set

    var latLng: LatLng = LatLng(41.0082, 28.9784) // Varsayılan İstanbul
    
    private var mockingJob: Job? = null
    private var gpsProviderAdded = false
    private var networkProviderAdded = false

    private val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return MockLocationBinder()
    }

    override fun onDestroy() {
        stopMockingLocation()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    fun toggleMocking() {
        if (isMocking) stopMockingLocation() else startMockingLocation()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sahte Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sahte konum aktif olduğunda bildirim gösterir"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sahte Konum Aktif")
            .setContentText("Konum: ${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    private fun startMockingLocation() {
        // Will be added to location history if not existing.
        StorageManager.addLocationToHistory(latLng)

        if (!isMocking) {
            isMocking = true
            
            // Foreground service olarak başlat
            startForeground(NOTIFICATION_ID, createNotification())
            
            mockingJob = GlobalScope.launch(Dispatchers.IO) {
                mockLocation()
            }
            Log.d(TAG, "Mock location started")
        }
    }

    private fun stopMockingLocation() {
        if (isMocking) {
            isMocking = false
            mockingJob?.cancel()
            mockingJob = null
            
            // Test provider'ları temizle
            cleanupTestProviders()
            
            // Foreground service'i durdur
            stopForeground(STOP_FOREGROUND_REMOVE)
            
            Log.d(TAG, "Mock location stopped")
        }
    }
    
    private fun cleanupTestProviders() {
        try {
            if (gpsProviderAdded) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
                gpsProviderAdded = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing GPS test provider: ${e.message}")
        }
        
        try {
            if (networkProviderAdded) {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER)
                networkProviderAdded = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Network test provider: ${e.message}")
        }
    }

    private fun addTestProvider(providerName: String): Boolean {
        val requiresNetwork = providerName == LocationManager.NETWORK_PROVIDER
        val requiresSatellite = providerName == LocationManager.GPS_PROVIDER
        val requiresCell = false
        val hasMonetaryCost = false
        val supportsAltitude = true
        val supportsSpeed = true
        val supportsBearing = true
        val powerRequirement = ProviderProperties.POWER_USAGE_HIGH
        val accuracy = ProviderProperties.ACCURACY_FINE

        try {
            // Önce varsa eski provider'ı kaldır
            try {
                locationManager.removeTestProvider(providerName)
            } catch (e: Exception) {
                // Provider yoksa hata verebilir, görmezden gel
            }
            
            locationManager.addTestProvider(
                providerName,
                requiresNetwork,
                requiresSatellite,
                requiresCell,
                hasMonetaryCost,
                supportsAltitude,
                supportsSpeed,
                supportsBearing,
                powerRequirement,
                accuracy
            )
            
            if (providerName == LocationManager.GPS_PROVIDER) {
                gpsProviderAdded = true
            } else if (providerName == LocationManager.NETWORK_PROVIDER) {
                networkProviderAdded = true
            }
            
            return true
        } catch (se: SecurityException) {
            val ctx = FakeGPSProApp.shared.applicationContext
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    ctx,
                    "Sahte konum başarısız oldu, bu uygulamayı sahte konum uygulaması olarak seçmelisiniz.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error adding test provider $providerName: ${e.message}")
            return false
        }
    }

    private suspend fun mockLocation() {
        // GPS ve Network provider'ları ekle
        val gpsAdded = addTestProvider(LocationManager.GPS_PROVIDER)
        val networkAdded = addTestProvider(LocationManager.NETWORK_PROVIDER)
        
        if (!gpsAdded && !networkAdded) {
            isMocking = false
            return
        }

        if (gpsAdded) {
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        }
        if (networkAdded) {
            locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        }

        while (isMocking) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = SystemClock.elapsedRealtimeNanos()
            
            // GPS Provider için mock location
            if (gpsAdded) {
                val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    altitude = 10.0
                    time = currentTime
                    accuracy = 1f
                    speed = 0f
                    bearing = 0f
                    elapsedRealtimeNanos = elapsedTime
                }
                
                try {
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLocation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting GPS location: ${e.message}")
                }
            }
            
            // Network Provider için mock location
            if (networkAdded) {
                val networkLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    altitude = 10.0
                    time = currentTime
                    accuracy = 1f
                    speed = 0f
                    bearing = 0f
                    elapsedRealtimeNanos = elapsedTime
                }
                
                try {
                    locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, networkLocation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting Network location: ${e.message}")
                }
            }
            
            // Daha sık güncelleme yap (100ms)
            kotlinx.coroutines.delay(100L)
        }
    }

    inner class MockLocationBinder : Binder() {
        fun getService(): MockLocationService {
            return this@MockLocationService
        }
    }
}
