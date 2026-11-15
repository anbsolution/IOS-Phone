package com.iosphone.dialer.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat

class PhoneInCallService : InCallService() {

    companion object {
        private const val CHANNEL_ID = "ios_phone_calls"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createCallNotificationChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallSession.attach(this, call)
        showCallNotification(call)
    }

    override fun onCallRemoved(call: Call) {
        CallSession.detach(call)
        getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
        super.onCallRemoved(call)
    }

    private fun createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phone calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming and ongoing phone calls"
                lockscreenVisibility =
                    android.app.Notification.VISIBILITY_PUBLIC
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun showCallNotification(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
            ?.takeIf { it.isNotBlank() } ?: "Unknown number"

        val incoming = call.state == Call.STATE_RINGING

        val screenIntent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            screenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(
                if (incoming) "Incoming call" else "Phone call"
            )
            .setContentText(number)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(
                if (incoming) pendingIntent else null,
                incoming
            )
            .build()

        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission may be unavailable.
        }
    }
}
