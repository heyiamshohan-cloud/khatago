package com.shohan.khatago.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Temporary placeholder so the toolchain can be verified end to end.
 * Replaced by the full navigation graph in the next commit.
 */
@Composable
fun KhataGoNavHost() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("KhataGo", style = MaterialTheme.typography.headlineMedium)
    }
}
