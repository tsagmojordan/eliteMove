package com.llr.rideapp.data.local

import com.llr.rideapp.utils.log

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREFS_FILE_NAME  = "rideapp_secure_prefs"
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_USER_ROLES    = "user_roles"
        private const val KEY_EXPIRES_AT    = "expires_at" // timestamp absolu en ms
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        log.debug("[TokenManager] --saveTokens")
        com.llr.rideapp.utils.log.debug("[TokenManager] --saveTokens")
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveUserId(userId: String) {
        log.debug("[TokenManager] --saveUserId")
        com.llr.rideapp.utils.log.debug("[TokenManager] --saveUserId")
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun saveRoles(roles: List<String>) {
        log.debug("[TokenManager] --saveRoles")
        com.llr.rideapp.utils.log.debug("[TokenManager] --saveRoles")
        sharedPreferences.edit().putString(KEY_USER_ROLES, roles.joinToString(",")).apply()
    }

    /** Stocke l'heure d'expiration absolue (System.currentTimeMillis + expiresIn * 1000). */
    fun saveExpiresAt(expiresAtMs: Long) {
        log.debug("[TokenManager] --saveExpiresAt")
        com.llr.rideapp.utils.log.debug("[TokenManager] --saveExpiresAt")
        sharedPreferences.edit().putLong(KEY_EXPIRES_AT, expiresAtMs).apply()
    }

    fun getAccessToken(): String?  = sharedPreferences.getString(KEY_ACCESS_TOKEN,  null)
    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String?       = sharedPreferences.getString(KEY_USER_ID,       null)
    fun getExpiresAt(): Long       = sharedPreferences.getLong(KEY_EXPIRES_AT, 0L)

    fun getRoles(): List<String> {
        log.debug("[TokenManager] --getRoles")
        com.llr.rideapp.utils.log.debug("[TokenManager] --getRoles")
        val rolesStr = sharedPreferences.getString(KEY_USER_ROLES, "") ?: ""
        return if (rolesStr.isEmpty()) emptyList() else rolesStr.split(",")
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    /** Retourne true si le token est expiré ou si l'heure d'expiration n'est pas connue. */
    fun isTokenExpired(): Boolean {
        log.debug("[TokenManager] --isTokenExpired")
        com.llr.rideapp.utils.log.debug("[TokenManager] --isTokenExpired")
        val expiresAt = getExpiresAt()
        return expiresAt == 0L || System.currentTimeMillis() >= expiresAt
    }

    fun clearAll() {
        log.debug("[TokenManager] --clearAll")
        com.llr.rideapp.utils.log.debug("[TokenManager] --clearAll")
        sharedPreferences.edit().clear().apply()
    }
}

