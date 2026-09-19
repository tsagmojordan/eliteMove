package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.NotificationApiService
import com.llr.rideapp.domain.model.AppNotification
import com.llr.rideapp.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApiService: NotificationApiService
) : NotificationRepository {

    override suspend fun getAllNotifications(): Result<List<AppNotification>> = safeApiCall {
        // Contrat C4 : ApiResponse<PaginatedResponse<...>> → données dans data.content
        val response = notificationApiService.getAllNotifications()
        val body = response.body() ?: throw Exception("Réponse de notifications vide")
        if (body.success == false) throw Exception(body.message ?: "Erreur chargement des notifications")
        body.data?.content?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun getUnreadCount(): Result<Int> = safeApiCall {
        // Contrat C4 : data est un Long brut
        val response = notificationApiService.getUnreadCount()
        val body = response.body() ?: throw Exception("Réponse du compteur vide")
        if (body.success == false) throw Exception(body.message ?: "Erreur compteur de notifications")
        body.data?.toInt() ?: 0
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = safeApiCall {
        // Contrat C4 : ApiResponse<Void> → succès = code 2xx uniquement
        val response = notificationApiService.markAsRead(notificationId)
        if (!response.isSuccessful) throw Exception("Erreur marquer comme lu (HTTP ${response.code()})")
        Unit
    }

    override suspend fun markAllAsRead(): Result<Unit> = safeApiCall {
        val response = notificationApiService.markAllAsRead()
        if (!response.isSuccessful) throw Exception("Erreur tout marquer comme lu (HTTP ${response.code()})")
        Unit
    }

    // Backend : champ `subject` → mappé vers le titre du modèle de domaine
    private fun com.llr.rideapp.data.remote.dto.NotificationDto.toModel() = AppNotification(
        id = id ?: "",
        title = subject ?: "",
        message = message ?: "",
        read = read ?: false,
        createdAt = createdAt ?: ""
    )
}