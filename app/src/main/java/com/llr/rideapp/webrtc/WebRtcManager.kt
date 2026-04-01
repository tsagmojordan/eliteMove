package com.llr.rideapp.webrtc

import android.content.Context
import org.webrtc.audio.JavaAudioDeviceModule
import com.llr.rideapp.domain.repository.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire WebRTC pour les appels audio/vidéo.
 * Gère la création du PeerConnection, l'échange des signaux SDP/ICE via le backend.
 */
@Singleton
class WebRtcManager @Inject constructor(
    private val callRepository: CallRepository
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var localSurfaceView: SurfaceViewRenderer? = null
    private var remoteSurfaceView: SurfaceViewRenderer? = null
    private var currentCallId: String? = null
    private var eglBase: EglBase? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun initialize(context: Context) {
        eglBase = EglBase.create()

        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val options = PeerConnectionFactory.Options()
        val audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase!!.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun getEglBase() = eglBase

    fun setRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        localSurfaceView = local
        remoteSurfaceView = remote
    }

    fun createOffer(callId: String, isVideo: Boolean, onOffer: (String) -> Unit) {
        currentCallId = callId
        setupPeerConnection()

        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            if (isVideo) {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        }

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                onOffer(sessionDescription.toJson())
            }
        }, mediaConstraints)
    }

    fun handleOffer(callId: String, offerSdpJson: String, onAnswer: (String) -> Unit) {
        currentCallId = callId
        setupPeerConnection()

        val sdp = SessionDescription(
            SessionDescription.Type.OFFER,
            extractSdpDescription(offerSdpJson)
        )
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)

        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                onAnswer(sessionDescription.toJson())
            }
        }, mediaConstraints)
    }

    fun handleAnswer(answerSdpJson: String) {
        val sdp = SessionDescription(
            SessionDescription.Type.ANSWER,
            extractSdpDescription(answerSdpJson)
        )
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
    }

    fun handleIceCandidate(iceCandidateJson: String) {
        try {
            val parts = iceCandidateJson
                .substringAfter("\"candidate\":\"").substringBefore("\"")
            val sdpMid = iceCandidateJson
                .substringAfter("\"sdpMid\":\"").substringBefore("\"")
            val sdpMLineIndex = iceCandidateJson
                .substringAfter("\"sdpMLineIndex\":").substringBefore(",").trim().toIntOrNull() ?: 0

            val candidate = IceCandidate(sdpMid, sdpMLineIndex, parts)
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            // Ignore malformed candidate
        }
    }

    fun sendSignalToBackend(signal: String) {
        val callId = currentCallId ?: return
        CoroutineScope(Dispatchers.IO).launch {
            callRepository.sendSignaling(callId, signal)
        }
    }

    fun endCall() {
        localAudioTrack?.dispose()
        localVideoTrack?.dispose()
        peerConnection?.close()
        peerConnection = null
        currentCallId = null
    }

    fun cleanup() {
        endCall()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
    }

    private fun setupPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    val candidateJson = """{"type":"candidate","candidate":"${candidate.sdp}","sdpMid":"${candidate.sdpMid}","sdpMLineIndex":${candidate.sdpMLineIndex}}"""
                    sendSignalToBackend(candidateJson)
                }

                override fun onAddStream(stream: MediaStream?) {
                    stream?.audioTracks?.firstOrNull()?.setEnabled(true)
                    stream?.videoTracks?.firstOrNull()?.addSink(remoteSurfaceView)
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        track.addSink(remoteSurfaceView)
                    }
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
            }
        )

        // Ajout de la piste audio locale
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)
        val localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    private fun extractSdpDescription(json: String): String {
        return try {
            json.substringAfter("\"sdp\":\"").substringBefore("\"")
                .replace("\\n", "\n").replace("\\r", "\r")
        } catch (e: Exception) {
            json
        }
    }

    private fun SessionDescription.toJson(): String {
        val typeStr = when (type) {
            SessionDescription.Type.OFFER -> "offer"
            SessionDescription.Type.ANSWER -> "answer"
            else -> "pranswer"
        }
        return """{"type":"$typeStr","sdp":"${description.replace("\n", "\\n").replace("\r", "\\r")}"}"""
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String) {}
    override fun onSetFailure(p0: String) {}
}
