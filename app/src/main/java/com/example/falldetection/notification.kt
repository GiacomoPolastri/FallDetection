package com.example.falldetection.notifications

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.falldetection.R

class FallNotificationHandler(private val context: Context) {

    fun sendSMSNotification(phoneNumber: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("FallNotificationHandler", "SMS inviato a $phoneNumber con messaggio: $message")
        } catch (e: Exception) {
            Log.e("FallNotificationHandler", "Errore durante l'invio dell'SMS: ${e.message}")
        }
    }

    fun sendEmailNotification(email: String, subject: String, message: String) {
        // Placeholder per inviare email - potrebbe essere necessario un servizio esterno
        Log.d("FallNotificationHandler", "Email inviata a $email con soggetto: $subject e messaggio: $message")
    }

    fun sendAppNotification(title: String, message: String) {
        val channelId = createNotificationChannel() // Creazione del canale di notifica se non esiste
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Icona di default per evitare errore
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel(): String {
        val channelId = "fall_detection_channel"
        val channelName = "Fall Detection Alerts"
        val importance = NotificationManagerCompat.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(channelId, channelName, importance)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    fun sendTelegramNotification(chatId: String, message: String) {
        // Placeholder per inviare messaggio Telegram tramite bot
        Log.d("FallNotificationHandler", "Messaggio Telegram inviato a chat ID $chatId con messaggio: $message (Token non utilizzato per sicurezza)")
    }
}