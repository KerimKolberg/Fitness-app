package com.kerimkolberg.fitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kerimkolberg.fitnessapp.model.Settings
import com.kerimkolberg.fitnessapp.model.ThemeMode
import com.kerimkolberg.fitnessapp.ui.AppNavHost
import com.kerimkolberg.fitnessapp.ui.theme.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FitnessApplication).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FitnessTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
