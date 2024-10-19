package com.example.falldetection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.falldetection.ui.theme.FallDetectionTheme

// La classe MainActivity estende ComponentActivity e implementa SensorEventListener per gestire i sensori
class MainActivity : ComponentActivity(), SensorEventListener {

    // Definizione del SensorManager per accedere ai sensori del dispositivo
    private lateinit var sensorManager: SensorManager
    // Variabile per il sensore accelerometro
    private var accelerometer: Sensor? = null

    // Variabili di stato per mantenere i valori dell'accelerometro e poterli mostrare sull'interfaccia utente
    private var xValue by mutableStateOf(0f)  // Valore sull'asse X
    private var yValue by mutableStateOf(0f)  // Valore sull'asse Y
    private var zValue by mutableStateOf(0f)  // Valore sull'asse Z

    // Metodo onCreate: eseguito alla creazione dell'attività
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilitazione Edge to Edge (opzionale per una UI che occupa tutto lo schermo)
        enableEdgeToEdge()

        // Inizializzazione del SensorManager per accedere ai sensori del dispositivo
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Ottenere il sensore accelerometro dal SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Verifica se l'accelerometro è disponibile e registra un listener per ricevere aggiornamenti
        accelerometer?.let {
            // Registra il listener per l'accelerometro
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            // Stampa un messaggio di errore se l'accelerometro non è disponibile
            Log.e("SensorError", "Accelerometro non disponibile")
        }

        // UI costruita usando Jetpack Compose
        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Chiamata al composable che visualizza i valori dell'accelerometro
                    AccelerometerDisplay(
                        x = xValue,
                        y = yValue,
                        z = zValue,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Metodo onSensorChanged: chiamato quando ci sono nuovi dati dal sensore
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Aggiorna i valori dell'accelerometro con i dati ricevuti
            xValue = event.values[0]  // Valore sull'asse X
            yValue = event.values[1]  // Valore sull'asse Y
            zValue = event.values[2]  // Valore sull'asse Z

            // Log dei valori per il debug
            Log.d("Accelerometro", "X: $xValue, Y: $yValue, Z: $zValue")
        }
    }

    // Metodo onAccuracyChanged: chiamato se l'accuratezza del sensore cambia (non usato in questo caso)
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Non necessario per ora, possiamo lasciare vuoto
    }
}

// Funzione Composable che visualizza i valori dell'accelerometro sull'interfaccia utente
@Composable
fun AccelerometerDisplay(x: Float, y: Float, z: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),  // Margine di 16dp intorno alla colonna
        verticalArrangement = Arrangement.Center,  // Allinea verticalmente al centro
        horizontalAlignment = Alignment.CenterHorizontally  // Allinea orizzontalmente al centro
    ) {
        // Titolo della sezione che mostra i valori dell'accelerometro
        Text(text = "Valori dell'Accelerometro", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))  // Spazio tra titolo e il primo valore
        // Visualizzazione del valore dell'asse X
        Text(text = "X: $x", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))  // Spazio tra i valori
        // Visualizzazione del valore dell'asse Y
        Text(text = "Y: $y", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))  // Spazio tra i valori
        // Visualizzazione del valore dell'asse Z
        Text(text = "Z: $z", style = MaterialTheme.typography.bodyLarge)
    }
}

// Funzione di anteprima per visualizzare l'interfaccia in Android Studio
@Preview(showBackground = true)
@Composable
fun AccelerometerDisplayPreview() {
    FallDetectionTheme {
        // Anteprima del composable con valori di esempio
        AccelerometerDisplay(x = 0.0f, y = 0.0f, z = 0.0f)
    }
}