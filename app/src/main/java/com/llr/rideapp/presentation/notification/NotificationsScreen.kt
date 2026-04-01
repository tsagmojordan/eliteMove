package com.llr.rideapp.presentation.notification

import com.llr.rideapp.utils.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.AppNotification
import com.llr.rideapp.domain.repository.NotificationRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var notifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        log.debug("[NotificationsScreen] --init")
        loadNotifications()
    }

    private fun loadNotifications() {
        log.debug("[NotificationsScreen] --loadNotifications")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = notificationRepository.getAllNotifications()
            isLoading = false
            result.fold(
                onSuccess = { notifications = it },
                onFailure = { error = it.message ?: "Erreur chargement notifications" }
            )
        }
    }

    fun markAsRead(notificationId: String) {
        log.debug("[NotificationsScreen] --markAsRead")
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)
            if (result.isSuccess) {
                // Mettre à jour localement
                notifications = notifications.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
            }
        }
    }

    fun markAllAsRead() {
        log.debug("[NotificationsScreen] --markAllAsRead")
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result.isSuccess) {
                // Mettre à jour localement
                notifications = notifications.map { it.copy(read = true) }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Notifications",
                onBack = onBack
            )

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (viewModel.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    ErrorText(message = viewModel.error!!)
                }
            } else if (viewModel.notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune notification.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        RideAppButton(
                            text = "Tout marquer comme lu",
                            onClick = { viewModel.markAllAsRead() },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                    }

                    items(viewModel.notifications) { notif ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { if (!notif.read) viewModel.markAsRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = if (!notif.read) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!notif.read) AccentGold else TextPrimary,
                                        fontSize = 18.sp
                                    )
                                    if (!notif.read) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(
                                                    ErrorRed,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = notif.message, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = notif.createdAt.substringBefore("T"),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
