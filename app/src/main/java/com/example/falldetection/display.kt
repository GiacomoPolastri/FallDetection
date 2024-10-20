package com.example.falldetection.display

import android.app.Activity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.falldetection.R

class DisplayHandler(private val activity: Activity) {

    private val rootView: ViewGroup = activity.findViewById(android.R.id.content)

    fun displayWelcomeMessage() {
        // Crea e mostra un messaggio di benvenuto all'avvio dell'applicazione
        val welcomeTextView = TextView(activity).apply {
            text = "Benvenuto nell'app di rilevamento cadute!"
            textSize = 18f
        }
        rootView.addView(welcomeTextView)
    }

    fun updateSensorStatus(status: String) {
        // Crea e aggiorna lo stato dei sensori nella UI
        val statusTextView = TextView(activity).apply {
            text = status
            textSize = 16f
        }
        rootView.addView(statusTextView)
    }

    fun showFallDetectedAlert() {
        // Crea e mostra un avviso sulla UI in caso di rilevamento di una caduta
        val alertTextView = TextView(activity).apply {
            text = "ATTENZIONE: Caduta rilevata!"
            textSize = 20f
            setTextColor(activity.resources.getColor(android.R.color.holo_red_dark, null))
        }
        rootView.addView(alertTextView)
    }
}
