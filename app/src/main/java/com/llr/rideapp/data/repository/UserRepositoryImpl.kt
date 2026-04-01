package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.UserApiService
import com.llr.rideapp.data.remote.dto.AssignRoleRequest
import com.llr.rideapp.domain.model.User
import com.llr.rideapp.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService
) : UserRepository {

    override suspend fun getAllUsers(search: String?): Result<List<User>> = safeApiCall {
        val response = userApiService.getAllUsers(search)
        response.body()?.data?.map { dto ->
            val roles = dto.roles?.mapNotNull { it.name } ?: emptyList()
            User(
                id = dto.id ?: "",
                firstname = dto.firstname ?: "",
                lastname = dto.lastname ?: "",
                username = dto.username ?: "",
                email = dto.email ?: "",
                enabled = dto.enabled ?: true,
                roles = roles
            )
        } ?: emptyList()
    }

    override suspend fun updateUserStatus(id: String, enabled: Boolean): Result<User> = safeApiCall {
        val response = userApiService.updateUserStatus(id, enabled)
        val dto = response.body()?.data ?: throw Exception("Erreur mise à jour de l'utilisateur")
        val roles = dto.roles?.mapNotNull { it.name } ?: emptyList()
        User(
            id = dto.id ?: "",
            firstname = dto.firstname ?: "",
            lastname = dto.lastname ?: "",
            username = dto.username ?: "",
            email = dto.email ?: "",
            enabled = dto.enabled ?: true,
            roles = roles
        )
    }

    override suspend fun assignRoles(id: String, roleIds: List<String>): Result<User> = safeApiCall {
        val response = userApiService.assignRoles(id, AssignRoleRequest(roleIds))
        val dto = response.body()?.data ?: throw Exception("Erreur assignation des rôles")
        val roles = dto.roles?.mapNotNull { it.name } ?: emptyList()
        User(
            id = dto.id ?: "",
            firstname = dto.firstname ?: "",
            lastname = dto.lastname ?: "",
            username = dto.username ?: "",
            email = dto.email ?: "",
            enabled = dto.enabled ?: true,
            roles = roles
        )
    }
}
