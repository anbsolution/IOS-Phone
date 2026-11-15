package com.iosphone.dialer.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object CallSession {
    var currentCall by mutableStateOf<Call?>(null)
        private set

    var callState by mutableStateOf(Call.STATE_DISCONNECTED)
        private set

    var phoneNumber by mutableStateOf("")
        private set

    var isMuted by mutableStateOf(false)
        private set

    var isSpeakerOn by mutableStateOf(false)
        private set

    private var service: InCallService? = null
    private var callback: Call.Callback? = null

    fun attach(inCallService: InCallService, call: Call) {
        service = inCallService

        callback?.let { old ->
            currentCall?.unregisterCallback(old)
        }

        currentCall = call
        callState = call.state
        phoneNumber = call.details.handle?.schemeSpecificPart.orEmpty()

        val newCallback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                callState = state
                phoneNumber =
                    call.details.handle?.schemeSpecificPart.orEmpty()
            }

            override fun onDetailsChanged(
                call: Call,
                details: Call.Details
            ) {
                phoneNumber =
                    details.handle?.schemeSpecificPart.orEmpty()
            }
        }

        callback = newCallback
        call.registerCallback(newCallback)
    }

    fun detach(call: Call) {
        if (currentCall !== call) return

        callback?.let { call.unregisterCallback(it) }
        callback = null
        currentCall = null
        callState = Call.STATE_DISCONNECTED
        phoneNumber = ""
        isMuted = false
        isSpeakerOn = false
        service = null
    }

    fun answer() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun decline() {
        currentCall?.reject(false, null)
    }

    fun endCall() {
        currentCall?.disconnect()
    }

    fun toggleHold() {
        when (callState) {
            Call.STATE_ACTIVE -> currentCall?.hold()
            Call.STATE_HOLDING -> currentCall?.unhold()
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        service?.setMuted(isMuted)
    }

    fun toggleSpeaker() {
        val next = !isSpeakerOn
        val route = if (next) {
            CallAudioState.ROUTE_SPEAKER
        } else {
            CallAudioState.ROUTE_EARPIECE
        }

        service?.setAudioRoute(route)
        isSpeakerOn = next
    }
}
