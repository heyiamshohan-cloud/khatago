package com.shohan.khatago.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.ui.components.KhataGoPrimaryButton
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

/**
 * First-run setup: the only thing KhataGo needs is a name. Currency is fixed to
 * Bangladeshi Taka and shown as information, not a choice, to keep setup short.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupScreen(
    onCompleted: () -> Unit,
    onSaveName: suspend (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var welcomed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(welcomed) {
        if (welcomed) {
            onSaveName(name.trim())
            delay(900)
            onCompleted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .imePadding()
            .padding(horizontal = Spacing.Gutter)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(Spacing.XXXL))

        AnimatedContent(
            targetState = welcomed,
            transitionSpec = {
                if (targetState) {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 4 })
                } else {
                    (fadeIn() + slideInVertically { -it / 4 }) togetherWith
                        (fadeOut() + slideOutVertically { it / 4 })
                }.using(SizeTransform(clip = false))
            },
            label = "setup_step"
        ) { isWelcomed ->
            if (isWelcomed) {
                WelcomePanel(name = name.trim())
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(KhataGoGreen, RoundedCornerShape(ShapeTokens.Large)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(Spacing.XXL))
                    Text(
                        text = "What's your name?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary
                    )
                    Spacer(Modifier.height(Spacing.XS))
                    Text(
                        text = "We'll use it to greet you on the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(Modifier.height(Spacing.XXXL))

                    KhataGoTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        label = "Your name",
                        placeholder = "Enter your name",
                        errorText = error,
                        imeAction = ImeAction.Done,
                        onImeAction = { },
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(Modifier.height(Spacing.XL))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite, RoundedCornerShape(ShapeTokens.Large))
                            .padding(horizontal = Spacing.L, vertical = Spacing.M),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Currency",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            Text(
                                text = "Bangladeshi Taka (${Money.CURRENCY_SYMBOL})",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Fixed currency",
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (!welcomed) {
            KhataGoPrimaryButton(
                text = "Continue",
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        error = "Please complete the required fields."
                        return@KhataGoPrimaryButton
                    }
                    saving = true
                    welcomed = true
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(Spacing.L))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun WelcomePanel(name: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(KhataGoGreenSoft, RoundedCornerShape(ShapeTokens.Large)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(Spacing.XXL))
        Text(
            text = "Welcome, $name",
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
        Spacer(Modifier.height(Spacing.S))
        Text(
            text = "Your KhataGo is ready. Add your first account whenever you are.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
    }
}
