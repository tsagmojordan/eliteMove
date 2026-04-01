package com.llr.rideapp.data.remote.interceptor

import com.llr.rideapp.utils.log

import com.google.gson.Gson
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.data.remote.dto.AuthResponse
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor qui ajoute automatiquement le JWT Bearer token à chaque requête.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        log.debug("[AuthInterceptor] --intercept")
        com.llr.rideapp.utils.log.debug("[AuthInterceptor] --intercept")
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val newRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(newRequest)
    }
}

/**
 * Authenticator OkHttp : appelé automatiquement lors d'une réponse 401.
 * Tente de rafraîchir le token via POST /api/v1/auth/refresh et relance la requête.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        log.debug("[AuthInterceptor] --authenticate")
        com.llr.rideapp.utils.log.debug("[AuthInterceptor] --authenticate")
        // Éviter une boucle infinie si le refresh échoue aussi
        if (response.request.header("X-Retry-With-Refresh") != null) {
            tokenManager.clearAll()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearAll()
            return null
        }

        // Appel synchrone au endpoint de refresh (sans intercepteur pour éviter la récursion)
        val refreshClient = OkHttpClient.Builder().build()
        val refreshBody = okhttp3.RequestBody.create(
            "application/json".toMediaType(),
            """{"refreshToken":"$refreshToken"}"""
        )
        val refreshRequest = Request.Builder()
            .url(
                "${response.request.url.scheme}://${response.request.url.host}" +
                ":${response.request.url.port}/api/v1/auth/refresh"
            )
            .post(refreshBody)
            .header("X-Retry-With-Refresh", "true")
            .build()

        return try {
            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyStr = refreshResponse.body?.string()

                // Le JSON contient le wrapper ApiResponse, on doit utiliser un TypeToken pour le parser
                val type = object : com.google.gson.reflect.TypeToken<com.llr.rideapp.data.remote.dto.ApiResponse<AuthResponse>>() {}.type
                val apiResponse: com.llr.rideapp.data.remote.dto.ApiResponse<AuthResponse>? = try {
                    gson.fromJson(bodyStr, type)
                } catch (e: Exception) { null }

                val authResponse = apiResponse?.data

                if (authResponse?.accessToken != null && authResponse.refreshToken != null) {
                    // Mise à jour du stockage sécurisé avec les nouvelles valeurs
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    tokenManager.saveUserId(authResponse.user.id)
                    tokenManager.saveRoles(authResponse.user.roles.map { it.name })
                    tokenManager.saveExpiresAt(
                        System.currentTimeMillis() + authResponse.expiresIn * 1_000L
                    )

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${authResponse.accessToken}")
                        .removeHeader("X-Retry-With-Refresh")
                        .build()
                } else {
                    tokenManager.clearAll()
                    null
                }
            } else {
                tokenManager.clearAll()
                null
            }
        } catch (e: Exception) {
            tokenManager.clearAll()
            null
        }
    }
}

