/*
 * 8 Audio onboarding experience.
 * Licensed under GPL-3.0; see LICENSE and upstream attribution notices.
 */
package moe.koiverse.archivetune.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val symbol: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val features: List<String>,
)

private val onboardingPages = listOf(
    OnboardingPage(
        symbol = "♫",
        eyebrow = "WELCOME TO 8 AUDIO",
        title = "Your music,\none place.",
        description = "Find tracks, explore artists, and keep your music close in one calm, personal space.",
        features = listOf("Online music search and streaming", "Artist, album, and playlist discovery", "A library that grows with you"),
    ),
    OnboardingPage(
        symbol = "◖▶",
        eyebrow = "MADE TO KEEP PLAYING",
        title = "Stay in the\nmoment.",
        description = "Keep listening while you move between apps. Your playback session, queue, and controls stay connected.",
        features = listOf("Background playback and media controls", "Queue, lyrics, and mini-player", "Downloads and local audio"),
    ),
    OnboardingPage(
        symbol = "✧",
        eyebrow = "MAKE IT YOURS",
        title = "A player that\nfeels like you.",
        description = "Tune the look and listening experience to your taste, without clutter or unnecessary setup.",
        features = listOf("Dynamic album-art colors", "Dark mode and pure black", "Appearance, blur, and animation controls"),
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = onboardingPages[pageIndex]
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.22f),
                        scheme.secondary.copy(alpha = 0.10f),
                        scheme.surface,
                    ),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surface.copy(alpha = 0.58f),
                border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.12f)),
                shadowElevation = 10.dp,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(scheme.primary.copy(alpha = 0.28f), scheme.secondary.copy(alpha = 0.12f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("8", fontSize = 40.sp, fontWeight = FontWeight.Black, color = scheme.primary)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = page.eyebrow,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 43.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 25.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = scheme.surface.copy(alpha = 0.62f),
                border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.11f)),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = scheme.primary.copy(alpha = 0.13f),
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(page.symbol, color = scheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Built around your listening", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    page.features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primary.copy(alpha = 0.16f),
                            ) {
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    Text("✓", color = scheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                onboardingPages.indices.forEach { index ->
                    Surface(
                        modifier = Modifier
                            .width(if (index == pageIndex) 24.dp else 7.dp)
                            .height(7.dp),
                        shape = CircleShape,
                        color = if (index == pageIndex) scheme.primary else scheme.onSurface.copy(alpha = 0.22f),
                    ) {}
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { if (pageIndex < onboardingPages.lastIndex) pageIndex++ else onFinish() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
            ) {
                Text(if (pageIndex == onboardingPages.lastIndex) "Start listening" else "Continue", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.12f)),
            ) {
                Text("Skip setup", color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
