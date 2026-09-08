package com.shohan.khatago.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.NegativeSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite

/** Confirmation dialog used for every destructive action. */
@Composable
fun KhataGoConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(ShapeTokens.XLarge),
            color = SurfaceWhite
        ) {
            Column(modifier = Modifier.padding(Spacing.XXL)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkPrimary
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = Spacing.S)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.XXL),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    KhataGoTextButton(text = "Cancel", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(Spacing.S))
                    KhataGoPrimaryButton(
                        text = confirmLabel,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataGoBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CanvasWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Spacing.L)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.XXL, vertical = Spacing.S)
                )
            }
            content()
        }
    }
}

/** Full-screen dialog used for the app lock. */
@Composable
fun KhataGoFullScreenDialog(content: @Composable () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasWhite)
        ) {
            content()
        }
    }
}

@Composable
fun KhataGoResultBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (isError) NegativeSoft else com.shohan.khatago.ui.theme.PositiveSoft
    val foreground = if (isError) com.shohan.khatago.ui.theme.OnNegativeSoft else com.shohan.khatago.ui.theme.OnPositiveSoft
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShapeTokens.Medium),
        color = background,
        contentColor = foreground
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M)
        )
    }
}

@Composable
fun KhataGoInfoTile(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = KhataGoGreen
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.S))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = InkSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.L)
    )
}

@Composable
fun VerticalSpace(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}

/** Keeps the danger colour discoverable for callers building delete affordances. */
val DangerColor: Color get() = Negative
