package com.shohan.khatago.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite

/**
 * Standard form layout: scrolling fields with a top bar and a save button that
 * stays reachable while the keyboard is open.
 */
@Composable
fun KhataGoFormScaffold(
    title: String,
    onBack: () -> Unit,
    saveLabel: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    saveEnabled: Boolean = true,
    error: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = CanvasWhite,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OutlineSoft)
                )
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter, vertical = Spacing.M)) {
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Negative,
                            modifier = Modifier.padding(bottom = Spacing.S)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (secondaryLabel != null && onSecondary != null) {
                            KhataGoTextButton(text = secondaryLabel, onClick = onSecondary)
                            Spacer(Modifier.width(Spacing.XS))
                        }
                        KhataGoPrimaryButton(
                            text = saveLabel,
                            onClick = onSave,
                            enabled = saveEnabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.windowInsetsBottomHeight(androidx.compose.foundation.layout.WindowInsets.navigationBars))
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            KhataGoTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                modifier = Modifier.padding(horizontal = Spacing.Gutter)
            )
            Spacer(Modifier.height(Spacing.S))
            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.Gutter,
                    vertical = Spacing.S
                ),
                content = content
            )
            Spacer(Modifier.height(Spacing.XXL))
        }
    }
}

/** A labelled block of fields, used to group long forms. */
@Composable
fun KhataGoFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = Spacing.M)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = Spacing.S, start = 2.dp)
        )
        content()
        Spacer(Modifier.height(Spacing.L))
    }
}

/** Read-only summary card shown at the top of payment forms. */
@Composable
fun KhataGoSummaryStrip(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    Text(
                        value,
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}
