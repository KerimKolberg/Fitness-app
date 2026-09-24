package com.kerimkolberg.fitnessapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kerimkolberg.fitnessapp.AppContainer
import com.kerimkolberg.fitnessapp.FitnessApplication

/** Builds a ViewModel factory that has access to the [AppContainer]. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: CreationExtras.(AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = checkNotNull(this[APPLICATION_KEY]) as FitnessApplication
        create(application.container)
    }
}
