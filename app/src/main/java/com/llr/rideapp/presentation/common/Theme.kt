package com.llr.rideapp.presentation.common

import com.llr.rideapp.utils.log

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Couleurs RideApp ─────────────────────────────────────────────────────────
val PrimaryBlack = Color(0xFF0D0D0D)
val PrimaryDark = Color(0xFF1A1A2E)
val AccentGold = Color(0xFFFFD700)
val AccentGoldLight = Color(0xFFFFE566)
val SurfaceCard = Color(0xFF16213E)
val SurfaceElevated = Color(0xFF0F3460)
val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFB0B0C3)
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningOrange = Color(0xFFFF9800)
val StatusCompleted = Color(0xFF4CAF50)
val StatusInProgress = Color(0xFF2196F3)
val StatusPending = Color(0xFFFF9800)
val StatusCancelled = Color(0xFFE53935)

// ─── Color Scheme Dark ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = PrimaryBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = AccentGoldLight,
    secondary = AccentGoldLight,
    onSecondary = PrimaryBlack,
    background = PrimaryDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun RideAppTheme(content: @Composable () -> Unit) {
    log.debug("[Theme] --RideAppTheme")
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

// ─── Status Color helper ──────────────────────────────────────────────────────
fun rideStatusColor(status: String): Color {
    log.debug("[Theme] --rideStatusColor")
    return when (status.uppercase()) {
        "COMPLETED" -> StatusCompleted
        "IN_PROGRESS", "ACCEPTED" -> StatusInProgress
        "PENDING", "REQUESTED" -> StatusPending
        "CANCELLED", "DECLINED" -> StatusCancelled
        else -> TextSecondary
    }
}
