package com.kkfittracking

import android.app.Application
import kotlinx.coroutines.launch

class FitnessApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.exerciseRepository.syncBuiltIns() }
    }
}
