package com.kkfittracking.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        setContent {
            WatchApp(viewModel, ambient)
        }
    }
}
