package com.shohan.khatago.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import Icons.Outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Lock
import com.shohan.khatago.ui.components.KhataGoGroupCard
import com.shohan.khatago.ui.components.KhataGoPrimaryButton
import com.shohan.khatago.ui.components.KhataGoResultBanner
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoSwitchRow
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite

/**
 * App Lock setup.
 *
 * The PIN itself is never stored — only a salted SHA-256 hash — and the device
 * biometric can be used as a convenience on top of the PIN.
 */
@Composable
fun AppLockScreen(
    hasPin: Boolean,
    lockEnabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onEnable: (String) -> Unit,
    onDisable: () -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(if (hasPin) Stage.MANAGE else Stage.ENTER) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = CanvasWhite) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL)
        ) {
            item {
                KhataGoTopBar(
                    title = "App Lock",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Gutter, vertical = Spacing.XL),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(KhataGoGreenSoft, RoundedCornerShape(ShapeTokens.Large)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = KhataGoGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(Spacing.L))
                    Text(
                        text = when (stage) {
                            Stage.ENTER -> "Choose a 4-digit PIN"
                            Stage.CONFIRM -> "Confirm your PIN"
                            Stage.MANAGE -> "App Lock is on"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.XS))
                    Text(
                        text = when (stage) {
                            Stage.ENTER -> "You'll enter this when KhataGo opens."
                            Stage.CONFIRM -> "Type the same PIN again."
                            Stage.MANAGE -> "Your records stay on this device. KhataGo never stores the PIN itself."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(Spacing.XXL))

                    if (stage == Stage.MANAGE) {
                        KhataGoGroupCard {
                            Column(modifier = Modifier.padding(horizontal = Spacing.L)) {
                                KhataGoSwitchRow(
                                    title = "Require PIN",
                                    subtitle = "Ask for the PIN when the app opens",
                                    checked = lockEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            pin = ""
                                            confirmPin = ""
                                            stage = Stage.ENTER
                                        } else {
                                            onDisable()
                                            message = "App Lock turned off."
                                        }
                                    }
                                )
                                com.shohan.khatago.ui.components.GroupDivider()
                                KhataGoSwitchRow(
                                    title = "Unlock with biometric",
                                    subtitle = if (biometricAvailable) {
                                        "Use your fingerprint or face instead of the PIN"
                                    } else {
                                        "No biometric is set up on this device"
                                    },
                                    checked = biometricEnabled && biometricAvailable,
                                    onCheckedChange = { enabled ->
                                        if (biometricAvailable) onBiometricChanged(enabled)
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.L))
                        KhataGoSecondaryButton(
                            text = "Change PIN",
                            onClick = {
                                pin = ""
                                confirmPin = ""
                                error = null
                                stage = Stage.ENTER
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PinDots(
                            length = 4,
                            filled = if (stage == Stage.CONFIRM) confirmPin.length else pin.length
                        )
                        Spacer(Modifier.height(Spacing.XXL))
                        PinKeypad(
                            onDigit = { digit ->
                                error = null
                                if (stage == Stage.ENTER && pin.length < 4) {
                                    pin += digit
                                    if (pin.length == 4) stage = Stage.CONFIRM
                                } else if (stage == Stage.CONFIRM && confirmPin.length < 4) {
                                    confirmPin += digit
                                }
                            },
                            onDelete = {
                                error = null
                                if (stage == Stage.CONFIRM && confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                } else if (stage == Stage.CONFIRM && confirmPin.isEmpty()) {
                                    stage = Stage.ENTER
                                    pin = pin.dropLast(1)
                                } else if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                            }
                        )
                        Spacer(Modifier.height(Spacing.XL))
                        KhataGoPrimaryButton(
                            text = if (stage == Stage.CONFIRM) "Save PIN" else "Continue",
                            onClick = {
                                if (stage == Stage.CONFIRM) {
                                    if (confirmPin == pin) {
                                        onEnable(pin)
                                        message = "App Lock is on."
                                        stage = Stage.MANAGE
                                    } else {
                                        error = "Those PINs don't match. Try again."
                                        confirmPin = ""
                                    }
                                }
                            },
                            enabled = (stage == Stage.CONFIRM && confirmPin.length == 4),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (error != null) {
                        Spacer(Modifier.height(Spacing.M))
                        KhataGoResultBanner(message = error ?: "", isError = true)
                    }
                    if (message != null) {
                        Spacer(Modifier.height(Spacing.M))
                        KhataGoResultBanner(message = message ?: "", isError = false)
                    }
                }
            }
        }
    }
}

private enum class Stage { ENTER, CONFIRM, MANAGE }

@Composable
private fun PinDots(length: Int, filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.L)) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        if (index < filled) KhataGoGreen else Color.Transparent,
                        CircleShape
                    )
                    .border(1.dp, if (index < filled) KhataGoGreen else OutlineSoft, CircleShape)
            )
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
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
                        "del" -> KeypadKey(label = "⌫", onClick = onDelete, isText = true)
                        else -> KeypadKey(label = key, onClick = { onDigit(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, onClick: () -> Unit, isText: Boolean = false) {
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
