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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.falldetection.ui.theme.FallDetectionTheme
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Variabili di stato per mantenere i valori dell'accelerometro e altri dettagli
    private var xValue by mutableStateOf(0f)
    private var yValue by mutableStateOf(0f)
    private var zValue by mutableStateOf(0f)
    private var totalAcceleration by mutableStateOf(0f)
    private var isFalling by mutableStateOf(false)
    private var fallTimestamp by mutableStateOf("")

    // Definizione della soglia per l'accelerazione lineare che indicherà una possibile caduta
    private val FALL_THRESHOLD = 25.0f  // Valore soglia per la caduta (in m/s^2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Inizializzazione del SensorManager per accedere ai sensori del dispositivo
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Verifica se l'accelerometro è disponibile e registra un listener per ricevere aggiornamenti
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            Log.e("SensorError", "Accelerometro non disponibile")
        }

        // UI costruita usando Jetpack Compose
        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AccelerometerDisplay(
                        x = xValue,
                        y = yValue,
                        z = zValue,
                        totalAcceleration = totalAcceleration,
                        isFalling = isFalling,
                        fallTimestamp = fallTimestamp,
                        onFalseAlarmClicked = { isFalling = false },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Ottieni i valori dell'accelerometro
            xValue = event.values[0]
            yValue = event.values[1]
            zValue = event.values[2]

            // Calcolo dell'accelerazione totale per determinare una caduta
            totalAcceleration = sqrt(xValue * xValue + yValue * yValue + zValue * zValue)

            // Controlla se l'accelerazione totale supera la soglia di caduta
            if (totalAcceleration > FALL_THRESHOLD) {
                isFalling = true
                // Memorizza l'ora della caduta usando SimpleDateFormat
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                fallTimestamp = sdf.format(Date())
            }

            // Log dei valori per il debug
            Log.d("Accelerometro", "X: $xValue, Y: $yValue, Z: $zValue, Accel Totale: $totalAcceleration, Caduta: $isFalling")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Non necessario per ora
    }
}

@Composable
fun AccelerometerDisplay(
    x: Float,
    y: Float,
    z: Float,
    totalAcceleration: Float,
    isFalling: Boolean,
    fallTimestamp: String,
    onFalseAlarmClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner per la caduta
        if (isFalling) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "CADUTA RILEVATA!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "È un falso allarme?",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onFalseAlarmClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "Sì, è un falso allarme",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Titolo della sezione che mostra i valori dell'accelerometro
        Text(text = "Valori dell'Accelerometro", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Visualizzazione del valore dell'asse X
        Text(text = "X: $x", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Visualizzazione del valore dell'asse Y
        Text(text = "Y: $y", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Visualizzazione del valore dell'asse Z
        Text(text = "Z: $z", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Visualizzazione dell'accelerazione totale
        Text(text = "Accelerazione Totale: $totalAcceleration m/s²", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Visualizzazione del timestamp della caduta, se rilevata
        if (isFalling) {
            Text(text = "Ora della Caduta: $fallTimestamp", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccelerometerDisplayPreview() {
    FallDetectionTheme {
        AccelerometerDisplay(
            x = 0.0f,
            y = 0.0f,
            z = 0.0f,
            totalAcceleration = 0.0f,
            isFalling = true,
            fallTimestamp = "15:30:00",
            onFalseAlarmClicked = {}
        )
    }
}
