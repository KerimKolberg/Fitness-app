package com.kkfittracking.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientLifecycleObserver

class MainActivity : ComponentActivity() {
    private val viewModel: WatchViewModel by viewModels()

    /** Dimmed always-on mode: the watch keeps showing the workout instead of going back to the watch face. */
    private var ambient by mutableStateOf(false)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            ambient = true
        }

        override fun onExitAmbient() {
            ambient = false
            viewModel.refresh()
        }
    }

    /** Heart rate from Android 16 (Wear OS 6) on is its own permission; before, it came with body sensors. */
    private val heartRatePermission =
        if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

    private val permissions = arrayOf(
        heartRatePermission,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reportPermissions() }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun reportPermissions() {
        viewModel.onPermissions(heartRate = granted(heartRatePermission), location = granted(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        // Heart rate, steps and GPS distance: asked once; everything else works without them.
        if (savedInstanceState == null && permissions.any { !granted(it) }) askPermissions.launch(permissions)
        reportPermissions()
        setContent {
            WatchApp(viewModel, ambient)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onVisible(true)
    }

    override fun onStop() {
        viewModel.onVisible(false)
        super.onStop()
    }
}
