package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.RoleApiService
import com.llr.rideapp.data.remote.api.UserApiService
import com.llr.rideapp.data.remote.dto.AssignRoleRequest
import com.llr.rideapp.data.remote.dto.RoleRequest
import com.llr.rideapp.domain.model.Role
import com.llr.rideapp.domain.model.User
import com.llr.rideapp.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val roleApiService: RoleApiService
) : UserRepository {

    override suspend fun getAllUsers(search: String?): Result<List<User>> = safeApiCall {
        // Contrat C5 : ApiResponse<PaginatedResponse<UserResponse>> → données dans data.content
        val response = userApiService.getAllUsers(search)
        val body = response.body() ?: throw Exception("Réponse utilisateurs vide")
        if (body.success == false) throw Exception(body.message ?: "Erreur chargement des utilisateurs")
        body.data?.content?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun updateUserStatus(id: String, enabled: Boolean): Result<User> = safeApiCall {
        val response = userApiService.updateUserStatus(id, enabled)
        val dto = response.body()?.data ?: throw Exception("Erreur mise à jour de l'utilisateur")
        dto.toModel()
    }

    override suspend fun assignRoles(id: String, roleIds: List<String>): Result<User> = safeApiCall {
        val response = userApiService.assignRoles(id, AssignRoleRequest(roleIds))
        val dto = response.body()?.data ?: throw Exception("Erreur assignation des rôles")
        dto.toModel()
    }

    override suspend fun deleteUser(id: String): Result<Unit> = safeApiCall {
        val response = userApiService.deleteUser(id)
        if (!response.isSuccessful) throw Exception("Erreur suppression de l'utilisateur (HTTP ${response.code()})")
        Unit
    }

    override suspend fun getRoles(search: String?): Result<List<Role>> = safeApiCall {
        val response = roleApiService.getRoles(search)
        val body = response.body() ?: throw Exception("Réponse rôles vide")
        if (body.success == false) throw Exception(body.message ?: "Erreur chargement des rôles")
        body.data?.content?.map {
            Role(id = it.id ?: "", name = it.name, description = it.description)
        } ?: emptyList()
    }

    override suspend fun createRole(name: String, description: String): Result<Role> = safeApiCall {
        val response = roleApiService.createRole(RoleRequest(name, description))
        val dto = response.body()?.data ?: throw Exception("Erreur création du rôle")
        Role(id = dto.id ?: "", name = dto.name, description = dto.description)
    }

    override suspend fun deleteRole(roleId: String): Result<Unit> = safeApiCall {
        val response = roleApiService.deleteRole(roleId)
        if (!response.isSuccessful) throw Exception("Erreur suppression du rôle (HTTP ${response.code()})")
        Unit
    }

    private fun com.llr.rideapp.data.remote.dto.UserDto.toModel() = User(
        id = id ?: "",
        firstname = firstname ?: "",
        lastname = lastname ?: "",
        username = username ?: "",
        email = email ?: "",
        enabled = enabled ?: true,
        roles = roles?.mapNotNull { it.name } ?: emptyList()
    )
}