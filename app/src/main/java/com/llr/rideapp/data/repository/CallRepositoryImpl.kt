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
        // Contrat C3 : réponse BRUTE InitiateCallResponse { callId, message }
        val response = callApiService.initiateCall(InitiateCallRequest(calleeId, callType))
        if (!response.isSuccessful) throw Exception("Erreur initiation appel (HTTP ${response.code()})")
        val body = response.body() ?: throw Exception("Réponse d'appel manquante")
        val callId = body.callId ?: throw Exception(body.message ?: "Identifiant d'appel manquant")
        Call(
            id = callId,
            callerId = "",
            calleeId = calleeId,
            callType = callType,
            status = "INITIATED",
            startedAt = null,
            endedAt = null
        )
    }

    override suspend fun acceptCall(callId: String): Result<Call> = safeApiCall {
        val response = callApiService.acceptCall(callId)
        if (!response.isSuccessful) throw Exception("Erreur accepter appel")
        // 204 No Content : on reflète le changement localement.
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
        // Réponse BRUTE List<CallResponse> (contrat C3) ; createdAt → startedAt
        val response = callApiService.getCallHistory(page, size)
        response.body()?.map { it.toModel() } ?: emptyList()
    }

    private fun com.llr.rideapp.data.remote.dto.CallDto.toModel() = Call(
        id = id ?: "",
        callerId = callerId ?: "",
        calleeId = calleeId ?: "",
        callType = callType ?: "AUDIO",
        status = status ?: "UNKNOWN",
        startedAt = createdAt,
        endedAt = endedAt
    )
}