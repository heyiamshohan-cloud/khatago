package com.shohan.khatago.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.khatago.core.di.AppContainer

/** The process-wide container, provided once at the root of the UI tree. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("KhataGo container was not provided")
}

/**
 * Creates a [ViewModel] from the explicit container — no reflection, no DI
 * framework, and every dependency is visible at the call site.
 */
@Composable
inline fun <reified VM : ViewModel> khataGoViewModel(
    crossinline creator: (AppContainer) -> VM
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator(container) as T
        }
    )
}
