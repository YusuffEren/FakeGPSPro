package com.yusuferen.mockgps

import android.app.Application
import com.yusuferen.mockgps.service.VibratorService
import com.yusuferen.mockgps.storage.StorageManager

class FakeGPSProApp : Application() {
    companion object {
        lateinit var shared: FakeGPSProApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        shared = this
        StorageManager.initialise(this)
        VibratorService.initialise(this)
    }

}