package com.example.kerasondevice

import android.app.Application
import android.util.Log

/**
 * Application entry point for the Keras On-Device demo.
 *
 * Currently only provides a lifecycle hook for debug logging. Future
 * initialisation (e.g. shared inference executors, telemetry) should be
 * added here rather than in individual Activities or ViewModels.
 */
class KerasOnDeviceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Keras On-Device application created")
    }

    companion object {
        private const val TAG = "KerasOnDeviceApp"
    }
}
