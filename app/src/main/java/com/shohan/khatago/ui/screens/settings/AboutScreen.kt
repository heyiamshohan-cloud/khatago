package com.shohan.khatago.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shohan.khatago.ui.components.GroupDivider
import com.shohan.khatago.ui.components.KhataGoGroupCard
import com.shohan.khatago.ui.components.KhataGoKeyValueRow
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing

/**
 * About KhataGo: brand, promise and developer details.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(containerColor = CanvasWhite) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                KhataGoTopBar(
                    title = "About",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Spacer(Modifier.height(Spacing.XL))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(KhataGoGreen, RoundedCornerShape(ShapeTokens.XLarge)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnHero
                    )
                }
            }

            item {
                Spacer(Modifier.height(Spacing.L))
                Text(
                    text = "KhataGo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "All your finances, in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }

            item {
                Spacer(Modifier.height(Spacing.XXL))
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoGroupCard {
                        Column(modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M)) {
                            KhataGoKeyValueRow(label = "Version", value = "1.0.0")
                            GroupDivider()
                            KhataGoKeyValueRow(label = "Created by", value = "Shohan Khan")
                            GroupDivider()
                            KhataGoKeyValueRow(label = "Contact", value = "helloiamshohan@gmail.com")
                            GroupDivider()
                            KhataGoKeyValueRow(label = "Price", value = "Free forever")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.XL))
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoGroupCard {
                        Column(modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M)) {
                            Text(
                                text = "Privacy",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                            Spacer(Modifier.height(Spacing.XS))
                            Text(
                                text = "KhataGo works offline and keeps every record on your device. " +
                                    "No ads, no subscriptions, no tracking, no external financial backend. " +
                                    "Nothing is uploaded unless you export a backup yourself.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.XXL))
                Text(
                    text = "Made with care for everyday money management.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Gutter)
                )
            }
        }
    }
}
