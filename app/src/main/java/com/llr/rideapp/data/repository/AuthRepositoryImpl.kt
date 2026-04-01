package com.llr.rideapp.data.repository

import com.llr.rideapp.utils.log

import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.data.remote.api.AuthApiService
import com.llr.rideapp.data.remote.api.UserApiService
import com.llr.rideapp.data.remote.dto.LoginRequest
import com.llr.rideapp.data.remote.dto.RegisterRequest
import com.llr.rideapp.domain.model.AuthToken
import com.llr.rideapp.domain.model.User
import com.llr.rideapp.domain.model.UserRole
import com.llr.rideapp.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userApiService: UserApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(usernameOrEmail: String, password: String): Result<AuthToken> {
        log.debug("[AuthRepositoryImpl] --login")
        com.llr.rideapp.utils.log.debug("[AuthRepositoryImpl] --login")
        return try {
            val response = authApiService.login(LoginRequest(usernameOrEmail, password))
            if (response.isSuccessful) {
                val wrapper = response.body()
                    ?: return Result.failure(Exception("Réponse de login vide"))

                // Le backend renvoie { success, message, data: { accessToken, ... }, status, timestamp }
                if (wrapper.success != true) {
                    return Result.failure(Exception(wrapper.message ?: "Échec de la connexion"))
                }

                val authData = wrapper.data
                    ?: return Result.failure(Exception("Données d'authentification manquantes"))

                // ✅ userId et roles viennent de authData.user — JAMAIS de authData directement
                val authToken = AuthToken(
                    accessToken  = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId       = authData.user.id,
                    roles        = authData.user.roles.map { it.name }
                )

                // Stockage sécurisé (EncryptedSharedPreferences)
                tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                tokenManager.saveUserId(authData.user.id)
                tokenManager.saveRoles(authData.user.roles.map { it.name })
                // expiresIn est en secondes → heure d'expiration absolue
                tokenManager.saveExpiresAt(
                    System.currentTimeMillis() + authData.expiresIn * 1_000L
                )

                Result.success(authToken)
            } else {
                val code = response.code()
                val msg  = response.message()
                Result.failure(Exception("Échec de la connexion : $code $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        firstname: String, lastname: String, username: String,
        email: String, password: String
    ): Result<Unit> {
        return try {
            val response = userApiService.register(
                RegisterRequest(firstname, lastname, username, email, password)
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Échec de l'inscription : ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        log.debug("[AuthRepositoryImpl] --logout")
        com.llr.rideapp.utils.log.debug("[AuthRepositoryImpl] --logout")
        return try {
            authApiService.logout()
            tokenManager.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            // On purge le cache local même si l'appel réseau échoue
            tokenManager.clearAll()
            Result.success(Unit)
        }
    }

    override suspend fun getUserById(id: String): Result<User> {
        log.debug("[AuthRepositoryImpl] --getUserById")
        com.llr.rideapp.utils.log.debug("[AuthRepositoryImpl] --getUserById")
        return try {
            val response = userApiService.getUserById(id)
            if (response.isSuccessful) {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("Utilisateur non trouvé"))
                val roles = dto.roles?.mapNotNull { it.name } ?: emptyList()
                Result.success(
                    User(
                        id        = dto.id       ?: "",
                        firstname = dto.firstname ?: "",
                        lastname  = dto.lastname  ?: "",
                        username  = dto.username  ?: "",
                        email     = dto.email     ?: "",
                        enabled   = dto.enabled   ?: true,
                        roles     = roles
                    )
                )
            } else {
                Result.failure(Exception("Erreur HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
