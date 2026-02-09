package com.yusuferen.mockgps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.yusuferen.mockgps.service.LocationHelper
import com.yusuferen.mockgps.service.MockLocationService
import com.yusuferen.mockgps.service.VibratorService
import com.yusuferen.mockgps.ui.screens.MapScreen
import com.yusuferen.mockgps.ui.theme.FakeGPSProTheme

class MainActivity : ComponentActivity() {
    private var mockLocationService: MockLocationService? = null
        private set(value) {
            field = value
            MockLocationService.instance = value
        }

    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MockLocationService.MockLocationBinder
            mockLocationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
        }
    }

    fun toggleMocking(): Boolean {
        if (isBound && LocationHelper.hasPermission(this)) {
            mockLocationService?.toggleMocking()
            if (mockLocationService?.isMocking == true) {
                Toast.makeText(this, "Konum sahtesi başlatıldı...", Toast.LENGTH_SHORT).show()
                VibratorService.vibrate()
                return true
            } else if (mockLocationService?.isMocking == false) {
                Toast.makeText(this, "Konum sahtesi durduruldu...", Toast.LENGTH_SHORT).show()
                VibratorService.vibrate()
                return false
            }
        } else if (!isBound && LocationHelper.hasPermission(this))
            Toast.makeText(this, "Servis bağlı değil", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this, "Konum izni verilmedi", Toast.LENGTH_SHORT).show()

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FakeGPSProTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(activity = this)
                }
            }
        }

        // Bind to the service (normal service olarak başlat, foreground sadece mocking başlayınca olacak)
        val serviceIntent = Intent(this, MockLocationService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind from the service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FakeGPSProTheme {
    }
}