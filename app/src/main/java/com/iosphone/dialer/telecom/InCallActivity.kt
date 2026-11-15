package com.iosphone.dialer.telecom

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(20, 24, 35)
        window.navigationBarColor = android.graphics.Color.rgb(20, 24, 35)

        setContent {
            MaterialTheme {
                CallScreen()
            }
        }
    }
}

@Composable
private fun CallScreen() {
    val number = CallSession.phoneNumber.ifBlank { "Unknown number" }
    val state = CallSession.callState

    val incoming = state == Call.STATE_RINGING
    val connected = state == Call.STATE_ACTIVE
    val onHold = state == Call.STATE_HOLDING

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF141823)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(Color(0xFF30394D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 48.sp)
                }

                Spacer(Modifier.height(24.dp))

                Text(number, color = Color.White, fontSize = 27.sp)
                Spacer(Modifier.height(10.dp))

                Text(
                    when {
                        incoming -> "Incoming call"
                        connected -> "Connected"
                        onHold -> "On hold"
                        state == Call.STATE_DIALING -> "Calling..."
                        state == Call.STATE_DISCONNECTED -> "Call ended"
                        else -> "Connecting..."
                    },
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
            }

            if (!incoming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControl("Mute", CallSession.isMuted) {
                        CallSession.toggleMute()
                    }
                    CallControl("Speaker", CallSession.isSpeakerOn) {
                        CallSession.toggleSpeaker()
                    }
                    CallControl("Hold", onHold) {
                        CallSession.toggleHold()
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (incoming) {
                    Button(
                        onClick = { CallSession.decline(); },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = { CallSession.answer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF24B65A)
                        )
                    ) {
                        Text("Answer")
                    }
                } else {
                    Button(
                        onClick = { CallSession.endCall() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("End Call")
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControl(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color(0xFF087DFA)
                else Color(0xFF30394D)
            )
        ) {
            Text(label)
        }
    }
}
