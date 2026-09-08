package com.shohan.khatago.ui.screens.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import Icons.Outlined.Fingerprint
import Icons.Outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.shohan.khatago.ui.LocalAppContainer
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock

/** True when the device can authenticate with a strong biometric or the device credential. */
@Composable
fun biometricAvailable(): Boolean {
    val context = LocalContext.current
    return remember {
        val manager = BiometricManager.from(context)
        manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

/**
 * Returns a launcher for the system biometric prompt, or null when no
 * FragmentActivity is hosting the UI.
 */
@Composable
fun rememberBiometricPrompt(
    onSuccess: () -> Unit,
    onError: (String) -> Unit
): (() -> Unit)? {
    val context = LocalContext.current
    val activity = LocalContext.current as? FragmentActivity ?: return null
    return {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // A rejected fingerprint: the system retries, so stay quiet.
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock KhataGo")
                .setSubtitle("Use your fingerprint, face or screen lock")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}

/**
 * The gate shown at startup when App Lock is on. The PIN is checked against the
 * stored salted hash — the PIN itself is never written to disk.
 */
@Composable
fun LockGate(onUnlocked: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val canBiometric = biometricAvailable()
    val launchBiometric = rememberBiometricPrompt(
        onSuccess = onUnlocked,
        onError = { }
    )

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    fun submit(value: String) {
        if (checking) return
        checking = true
        scope.launch {
            if (container.settingsRepository.verifyPin(value)) {
                onUnlocked()
            } else {
                error = "That PIN doesn't match. Try again."
                pin = ""
            }
            checking = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(KhataGoGreenSoft, androidx.compose.foundation.shape.RoundedCornerShape(ShapeTokens.Large)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = KhataGoGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(Spacing.L))
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.XS))
            Text(
                text = "Enter your PIN to open KhataGo.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.XXL))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.L)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (index < pin.length) KhataGoGreen else Color.Transparent,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (index < pin.length) KhataGoGreen else OutlineSoft,
                                CircleShape
                            )
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(Spacing.M))
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Negative
                )
            }
            Spacer(Modifier.height(Spacing.XXL))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                keys.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.XL, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(Modifier.size(64.dp))
                                "del" -> GateKey(
                                    label = "⌫",
                                    isText = true,
                                    onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                )
                                else -> GateKey(
                                    label = key,
                                    onClick = {
                                        if (pin.length < 4) {
                                            val next = pin + key
                                            pin = next
                                            if (next.length == 4) submit(next)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (canBiometric && launchBiometric != null) {
                Spacer(Modifier.height(Spacing.XL))
                IconButton(onClick = { launchBiometric() }) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = "Unlock with biometric",
                        tint = KhataGoGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Use your fingerprint or screen lock",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary
                )
            }
        }
    }
}

@Composable
private fun GateKey(label: String, onClick: () -> Unit, isText: Boolean = false) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (isText) Color.Transparent else SurfaceWhite)
            .then(if (isText) Modifier else Modifier.border(1.dp, OutlineSoft, CircleShape))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (isText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = if (isText) InkTertiary else InkPrimary
        )
    }
}
