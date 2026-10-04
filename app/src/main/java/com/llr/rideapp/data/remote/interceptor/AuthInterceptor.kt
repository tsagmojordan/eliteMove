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
 *
 * Garanties :
 * 1. Single-flight — un seul refresh réseau à la fois ; les requêtes 401 parallèles
 *    attendent le verrou puis rejouent avec le token déjà renouvelé (le backend révoque
 *    tous les tokens à chaque refresh, deux refreshs concurrents s'invalideraient).
 * 2. Anti-boucle — la requête relancée porte le marqueur X-Retry-With-Refresh ; si elle
 *    reçoit encore 401, on abandonne au lieu de re-raffraîchir indéfiniment.
 * 3. Purge conditionnelle — on ne purge les tokens QUE si le backend répond 401/403
 *    (refresh token réellement invalide). Un timeout ou un 5xx ne déconnecte pas.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        log.debug("[AuthInterceptor] --authenticate")
        com.llr.rideapp.utils.log.debug("[AuthInterceptor] --authenticate")
        // Anti-boucle : cette requête a déjà été relancée avec un token frais → abandon
        if (response.request.header("X-Retry-With-Refresh") != null) {
            tokenManager.clearAll()
            return null
        }

        // Le refresh token est obligatoire pour tenter quoi que ce soit
        if (tokenManager.getRefreshToken() == null) {
            tokenManager.clearAll()
            return null
        }

        synchronized(refreshLock) {
            // Pendant qu'un autre thread rafraîchissait, le token a pu être renouvelé :
            // si le token de la requête échouée n'est plus le token courant, on rejoue
            // directement avec ce dernier sans refaire d'appel réseau.
            val currentAccess = tokenManager.getAccessToken()
            val failedAuthHeader = response.request.header("Authorization")
            val failedToken = failedAuthHeader?.removePrefix("Bearer ")
            if (currentAccess != null && failedToken != null && currentAccess != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .header("X-Retry-With-Refresh", "true")
                    .build()
            }

            return doRefresh(response)
        }
    }

    /** Appel réseau de refresh proprement dit — invoqué sous verrou uniquement. */
    private fun doRefresh(response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearAll()
            return null
        }

        // Appel synchrone au endpoint de refresh (sans intercepteur pour éviter la récursion)
        // Contrat C1 : le refresh token est envoyé dans le header Authorization: Bearer <refresh>,
        // PAS dans un body JSON.
        val refreshClient = OkHttpClient.Builder().build()
        val refreshBody = okhttp3.RequestBody.create(
            "application/json".toMediaType(),
            ""
        )
        val refreshRequest = Request.Builder()
            .url(
                "${response.request.url.scheme}://${response.request.url.host}" +
                ":${response.request.url.port}/api/v1/auth/refresh"
            )
            .post(refreshBody)
            .header("Authorization", "Bearer $refreshToken")
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
                        .header("X-Retry-With-Refresh", "true")
                        .build()
                } else {
                    // Réponse 2xx mais illisible : on n'efface pas les tokens,
                    // la requête échouera et pourra être retentée plus tard.
                    null
                }
            } else {
                // Déconnexion uniquement si le refresh token est rejeté comme invalide.
                // 5xx / timeout → on garde la session, l'utilisateur peut retenter.
                if (refreshResponse.code == 401 || refreshResponse.code == 403) {
                    tokenManager.clearAll()
                }
                null
            }
        } catch (e: Exception) {
            // Erreur réseau pendant le refresh : on ne purge PAS les tokens.
            null
        }
    }
}

