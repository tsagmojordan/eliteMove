package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.CallApiService
import com.llr.rideapp.data.remote.dto.EndCallRequest
import com.llr.rideapp.data.remote.dto.InitiateCallRequest
import com.llr.rideapp.data.remote.dto.SignalingRequest
import com.llr.rideapp.domain.model.Call
import com.llr.rideapp.domain.repository.CallRepository
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(
    private val callApiService: CallApiService
) : CallRepository {

    override suspend fun initiateCall(calleeId: String, callType: String): Result<Call> = safeApiCall {
        val response = callApiService.initiateCall(InitiateCallRequest(calleeId, callType))
        response.body()?.data?.toModel() ?: throw Exception("Réponse d'appel manquante")
    }

    override suspend fun acceptCall(callId: String): Result<Call> = safeApiCall {
        val response = callApiService.acceptCall(callId)
        if (!response.isSuccessful) throw Exception("Erreur accepter appel")
        // Return a dummy call object or fetch the actual call status if necessary.
        // For 204 No Content, we just reflect the change locally.
        Call(id = callId, callerId = "", calleeId = "", callType = "AUDIO", status = "ACCEPTED", startedAt = null, endedAt = null)
    }

    override suspend fun declineCall(callId: String): Result<Call> = safeApiCall {
        val response = callApiService.declineCall(callId)
        if (!response.isSuccessful) throw Exception("Erreur décliner appel")
        Call(id = callId, callerId = "", calleeId = "", callType = "AUDIO", status = "DECLINED", startedAt = null, endedAt = null)
    }

    override suspend fun endCall(callId: String): Result<Call> = safeApiCall {
        val response = callApiService.endCall(callId, EndCallRequest())
        if (!response.isSuccessful) throw Exception("Erreur terminer appel")
        Call(id = callId, callerId = "", calleeId = "", callType = "AUDIO", status = "ENDED", startedAt = null, endedAt = null)
    }

    override suspend fun sendSignaling(callId: String, signal: String): Result<Unit> = safeApiCall {
        val response = callApiService.sendSignaling(SignalingRequest(callId, signal))
        if (!response.isSuccessful) throw Exception("Erreur envoyer signalisation")
        Unit
    }

    override suspend fun getCallHistory(page: Int, size: Int): Result<List<Call>> = safeApiCall {
        val response = callApiService.getCallHistory(page, size)
        response.body()?.data?.map { it.toModel() } ?: emptyList()
    }

    private fun com.llr.rideapp.data.remote.dto.CallDto.toModel() = Call(
        id = id ?: "",
        callerId = callerId ?: "",
        calleeId = calleeId ?: "",
        callType = callType ?: "AUDIO",
        status = status ?: "UNKNOWN",
        startedAt = startedAt,
        endedAt = endedAt
    )
}
