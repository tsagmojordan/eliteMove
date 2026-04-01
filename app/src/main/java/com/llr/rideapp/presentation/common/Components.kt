package com.llr.rideapp.presentation.common

import com.llr.rideapp.utils.log

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

// ─── LoadingOverlay ───────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay() {
    log.debug("[Components] --LoadingOverlay")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AccentGold)
    }
}

// ─── RideAppButton ────────────────────────────────────────────────────────────

@Composable
fun RideAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentGold,
            contentColor = PrimaryBlack,
            disabledContainerColor = AccentGold.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = PrimaryBlack,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ─── RideAppTextField ─────────────────────────────────────────────────────────

@Composable
fun RideAppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = if (leadingIcon != null) {
            { Icon(imageVector = leadingIcon, contentDescription = null, tint = AccentGold) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible)
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGold,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
            focusedLabelColor = AccentGold,
            unfocusedLabelColor = TextSecondary,
            cursorColor = AccentGold,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// ─── AppCard ─────────────────────────────────────────────────────────────────

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    )
}

// ─── StatusBadge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String) {
    log.debug("[Components] --StatusBadge")
    val color = rideStatusColor(status)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── TopBar with Notification Bell ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideAppTopBar(
    title: String? = null,
    showLogo: Boolean = false,
    unreadCount: Int = 0,
    onNotificationClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            if (showLogo) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.llr.rideapp.R.drawable.logo_app),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else if (title != null) {
                Text(
                    text = title,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Retour", tint = AccentGold)
                }
            }
        },
        actions = {
            if (onNotificationClick != null) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(containerColor = ErrorRed) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = AccentGold)
                    }
                }
            }
            if (onLogout != null) {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Filled.Logout, contentDescription = "Déconnexion", tint = TextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryDark
        )
    )
}

// ─── ErrorText ───────────────────────────────────────────────────────────────

@Composable
fun ErrorText(message: String) {
    log.debug("[Components] --ErrorText")
    Text(
        text = message,
        color = ErrorRed,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

// ─── GradientBackground ──────────────────────────────────────────────────────

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryBlack, PrimaryDark, SurfaceElevated)
                )
            ),
        content = content
    )
}
