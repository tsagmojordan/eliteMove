package com.llr.rideapp.data.repository

import com.llr.rideapp.utils.log

suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
    log.debug("[SafeApiCall] --safeApiCall")
    return try {
        Result.success(call())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
