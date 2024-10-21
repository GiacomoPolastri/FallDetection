package com.example.falldetection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.falldetection.ui.theme.FallDetectionTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.*
import android.telephony.SmsManager

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var xValue by mutableStateOf(0f)
    private var yValue by mutableStateOf(0f)
    private var zValue by mutableStateOf(0f)
    private var totalAcceleration by mutableStateOf(0f)
    private var isFalling by mutableStateOf(false)
    private var fallTimestamp by mutableStateOf("")
    private var monitoringFall by mutableStateOf(false)
    private var lastKnownLocation by mutableStateOf("")

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val fallThreshold = 25.0f
    private val yThreshold = 5.0f
    private val fallMonitoringTime = 10_000L

    private val channelID = "fall_detection_channel"
    private val notificationID = 1

    private val handler = Handler(Looper.getMainLooper())
    private val fallMonitorRunnable = Runnable {
        if (yValue in -yThreshold..yThreshold) {
            isFalling = true
            Log.d("FallDetection", "Caduta confermata dopo il controllo di 10 secondi.")

            // Invia SMS ai contatti di emergenza poiché la caduta è confermata
            val emergencyContact = "+39"  // Contatto di emergenza fisso per ora
            sendEmergencySms(emergencyContact)

        } else {
            isFalling = false
            monitoringFall = false
            Log.d("FallDetection", "Falso positivo rilevato, caduta annullata.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            Log.e("SensorError", "Accelerometro non disponibile")
        }

        createNotificationChannel()

        requestNotificationPermission()

        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Header(modifier = Modifier.fillMaxWidth())
                        AccelerometerDisplay(
                            x = xValue,
                            y = yValue,
                            z = zValue,
                            totalAcceleration = totalAcceleration,
                            isFalling = isFalling,
                            fallTimestamp = fallTimestamp,
                            lastKnownLocation = lastKnownLocation,
                            onFalseAlarmClicked = {
                                isFalling = false
                                monitoringFall = false
                                handler.removeCallbacks(fallMonitorRunnable)
                                cancelNotification()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            xValue = event.values[0]
            yValue = event.values[1]
            zValue = event.values[2]

            totalAcceleration = sqrt(xValue * xValue + yValue * yValue + zValue * zValue)

            if (totalAcceleration > fallThreshold && !monitoringFall) {
                isFalling = true
                monitoringFall = true

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                fallTimestamp = sdf.format(Date())

                getCurrentLocation()

                handler.postDelayed(fallMonitorRunnable, fallMonitoringTime)

                sendFallNotification()

                Log.d("FallDetection", "Possibile caduta rilevata. Inizio monitoraggio per 10 secondi.")
            }

            Log.d("Accelerometro", "X: $xValue, Y: $yValue, Z: $zValue, Accel Totale: $totalAcceleration, Caduta: $isFalling")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            val location = task.result
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                lastKnownLocation = "Latitudine: $lat, Longitudine: $lon"
                Log.d("FallDetection", "Posizione corrente: $lastKnownLocation")
            } else {
                lastKnownLocation = "Posizione non disponibile"
                Log.d("FallDetection", "Non è stato possibile ottenere la posizione corrente.")
            }
        }
    }

    private fun createNotificationChannel() {
            val name = "Rilevamento Caduta"
            val descriptionText = "Notifiche per rilevamento caduta"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2
                )
            }
        }
    }

    private fun sendFallNotification() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Caduta Rilevata")
            .setContentText("È stata rilevata una caduta. Apri l'app per confermare.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationID, builder.build())
        }
    }

    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(notificationID)
        }
    }

    // Funzione per inviare un messaggio SMS al contatto di emergenza
    private fun sendEmergencySms(contactNumber: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                3
            )
            return
        }

        try {
            val message = "Attenzione: è stata rilevata una possibile caduta. Posizione: $lastKnownLocation"
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager?.sendTextMessage(contactNumber, null, message, null, null)
            Log.d("FallDetection", "SMS di emergenza inviato a $contactNumber")
        } catch (e: Exception) {
            Log.e("FallDetection", "Errore durante l'invio dell'SMS: ${e.message}")
        }
    }

}

@Composable
fun Header(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF00CED1))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Fall Detection App",
            color = Color(0xFF00008B),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
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
    lastKnownLocation: String,
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

        Text(text = "Valori dell'Accelerometro", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "X: $x", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Y: $y", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Z: $z", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Accelerazione Totale: $totalAcceleration m/s²", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (isFalling) {
            Text(text = "Ora della Caduta: $fallTimestamp", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Posizione: $lastKnownLocation", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccelerometerDisplayPreview() {
    FallDetectionTheme {
        Column {
            Header()
            AccelerometerDisplay(
                x = 0.0f,
                y = 0.0f,
                z = 0.0f,
                totalAcceleration = 0.0f,
                isFalling = true,
                fallTimestamp = "15:30:00",
                lastKnownLocation = "Latitudine: 45.0, Longitudine: 9.0",
                onFalseAlarmClicked = {}
            )
        }
    }
}