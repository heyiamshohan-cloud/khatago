package com.shohan.khatago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shohan.khatago.ui.theme.KhataGoTheme

/**
 * KhataGo hosts a single Compose activity; every screen is a destination in the
 * KhataGo navigation graph (see ui/navigation/KhataGoNavHost.kt).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }
        enableEdgeToEdge()

        setContent {
            KhataGoTheme {
                com.shohan.khatago.ui.navigation.KhataGoNavHost()
            }
        }
    }
}
