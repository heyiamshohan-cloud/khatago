package com.shohan.khatago

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity

/**
 * KhataGo hosts a single Compose activity; every screen is a destination in the
 * KhataGo navigation graph (see ui/navigation/KhataGoNavHost.kt).
 *
 * [FragmentActivity] is used so the App Lock can hand off to the system
 * biometric prompt; nothing else in the app depends on fragments.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }
        enableEdgeToEdge()

        // TEMPORARY (bisection): ui package moved to staging/
        val container = (application as KhataGoApplication).container
    }
}
