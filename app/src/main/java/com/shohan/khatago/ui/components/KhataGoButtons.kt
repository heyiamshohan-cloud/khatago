package com.shohan.khatago.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.OutlineStrong
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.SizeTokens

/**
 * Buttons. Primary actions are 52dp tall with a 14dp radius — comfortable on a
 * 360dp phone and never ambiguous about where to tap.
 */

@Composable
fun KhataGoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = SizeTokens.MinTouch),
        enabled = enabled,
        shape = RoundedCornerShape(ShapeTokens.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = KhataGoGreen,
            contentColor = OnHero,
            disabledContainerColor = KhataGoGreen.copy(alpha = 0.35f),
            disabledContentColor = OnHero
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

@Composable
fun KhataGoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = SizeTokens.MinTouch),
        enabled = enabled,
        shape = RoundedCornerShape(ShapeTokens.Medium),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = KhataGoGreen
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

@Composable
fun KhataGoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = SizeTokens.MinTouch),
        enabled = enabled,
        shape = RoundedCornerShape(ShapeTokens.Medium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (color == Color.Unspecified) {
                MaterialTheme.colorScheme.primary
            } else {
                color
            }
        )
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.size(8.dp)
        )
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun KhataGoBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(SizeTokens.MinTouch)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = com.shohan.khatago.ui.theme.InkPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun KhataGoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = InkSecondary
) {
    IconButton(onClick = onClick, modifier = modifier.size(SizeTokens.MinTouch)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

object ButtonTokens {
    val OutlineColor = OutlineStrong
}
