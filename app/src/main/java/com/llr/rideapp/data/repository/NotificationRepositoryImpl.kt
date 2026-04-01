package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.NotificationApiService
import com.llr.rideapp.domain.model.AppNotification
import com.llr.rideapp.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApiService: NotificationApiService
) : NotificationRepository {

    override suspend fun getAllNotifications(): Result<List<AppNotification>> = safeApiCall {
        val response = notificationApiService.getAllNotifications()
        response.body()?.data?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun getUnreadCount(): Result<Int> = safeApiCall {
        val response = notificationApiService.getUnreadCount()
        val data = response.body()?.data
        data?.count ?: data?.unreadCount ?: 0
    }

    override suspend fun markAsRead(notificationId: String): Result<AppNotification> = safeApiCall {
        val response = notificationApiService.markAsRead(notificationId)
        response.body()?.data?.toModel() ?: throw Exception("Erreur marquer comme lu")
    }

    override suspend fun markAllAsRead(): Result<Unit> = safeApiCall {
        notificationApiService.markAllAsRead()
        Unit
    }

    private fun com.llr.rideapp.data.remote.dto.NotificationDto.toModel() = AppNotification(
        id = id ?: "",
        title = title ?: "",
        message = message ?: "",
        read = read ?: false,
        createdAt = createdAt ?: ""
    )
}
