package com.example.falldetection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
// import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
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
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.falldetection.ui.theme.FallDetectionTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var xValue by mutableFloatStateOf(0f)
    private var yValue by mutableFloatStateOf(0f)
    private var zValue by mutableFloatStateOf(0f)
    private var totalAcceleration by mutableFloatStateOf(0f)
    private var isFalling by mutableStateOf(false)
    private var fallTimestamp by mutableStateOf("")
    private var monitoringFall by mutableStateOf(false)
    private var lastKnownLocation by mutableStateOf("")
    private var emergencyContact by mutableStateOf("")
    private var emergencyEmail by mutableStateOf("")

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val FALL_THRESHOLD = 25.0f
    private val Y_THRESHOLD = 4.0f
    private val FALL_MONITORING_TIME = 10_000L

    private val CHANNEL_ID = "fall_detection_channel"
    private val NOTIFICATION_ID = 1

    private val handler = Handler(Looper.getMainLooper())
    private val fallMonitorRunnable = Runnable {
        if (yValue in -Y_THRESHOLD..Y_THRESHOLD) {
            isFalling = true
            Log.d("FallDetection", "Fall confirmed after 10 seconds of monitoring.")

            // Invia SMS e Email ai contatti di emergenza poiché la caduta è confermata
            sendEmergencySms(emergencyContact)
            sendEmergencyEmailUsingJavaMail(emergencyEmail)
            makeEmergencyCall(emergencyContact)
        } else {
            isFalling = false
            monitoringFall = false
            Log.d("FallDetection", "False alarm, fall cancelled.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            Log.e("SensorError", "Accelerometer not available")
        }

        createNotificationChannel()

        // Carica il numero di emergenza e l'email salvati in SharedPreferences
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        emergencyContact = sharedPref.getString("emergency_contact", "") ?: ""
        emergencyEmail = sharedPref.getString("emergency_email", "") ?: ""

        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Header(modifier = Modifier.fillMaxWidth())

                        EmergencyContactInput(
                            emergencyContact = emergencyContact,
                            emergencyEmail = emergencyEmail,
                            onEmergencyContactChanged = { contact ->
                                emergencyContact = contact
                                // Salva il numero di emergenza in SharedPreferences
                                with(sharedPref.edit()) {
                                    putString("emergency_contact", emergencyContact)
                                    apply()
                                }
                            },
                            onEmergencyEmailChanged = { email ->
                                emergencyEmail = email
                                // Salva l'email di emergenza in SharedPreferences
                                with(sharedPref.edit()) {
                                    putString("emergency_email", emergencyEmail)
                                    apply()
                                }
                            }
                        )

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

    private fun loadCredentials(): Pair<String, String> {
        val properties = Properties()
        return try {
            val inputStream = assets.open("config.properties")
            properties.load(inputStream)

            // Add a log to check if the file is loaded correctly
            Log.d("FallDetection", "Configuration file loaded successfully")

            val username = properties.getProperty("email.username")
            val password = properties.getProperty("email.password")

            // Verify if properties are read correctly
            Log.d("FallDetection", "Username: $username")
            Log.d("FallDetection", "Password: $password")

            Pair(username ?: "", password ?: "")
        } catch (e: IOException) {
            Log.e("FallDetection", "Error while loading configuration file: ${e.message}")
            Pair("", "")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            xValue = event.values[0]
            yValue = event.values[1]
            zValue = event.values[2]

            totalAcceleration = sqrt(xValue * xValue + yValue * yValue + zValue * zValue)

            if (totalAcceleration > FALL_THRESHOLD && !monitoringFall) {
                isFalling = true
                monitoringFall = true

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                fallTimestamp = sdf.format(Date())

                getCurrentLocation()

                handler.postDelayed(fallMonitorRunnable, FALL_MONITORING_TIME)

                sendFallNotification()

                Log.d(
                    "FallDetection",
                    "Possible fall detected. Starting monitoring for 10 seconds."
                )
            }

            Log.d(
                "Accelerometer",
                "X: $xValue, Y: $yValue, Z: $zValue, Total Accel: $totalAcceleration, Falling: $isFalling"
            )
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
                lastKnownLocation = "Latitude: $lat, Longitude: $lon"
                Log.d("FallDetection", "Current location: $lastKnownLocation")
            } else {
                lastKnownLocation = "Location not available"
                Log.d("FallDetection", "Unable to get current location.")
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Fall Detection"
        val descriptionText = "Notifications for fall detection"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendFallNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    4
                )
                return
            }
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

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Fall Detected")
            .setContentText("A fall has been detected. Open the app to confirm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 1000, 500))
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun sendEmergencySms(contactNumber: String) {
        if (contactNumber.isEmpty()) {
            Log.e("FallDetection", "No emergency contact configured.")
            return
        }

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
            val message = "Warning: A possible fall has been detected. Location: $lastKnownLocation"
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager?.sendTextMessage(contactNumber, null, message, null, null)
            Log.d("FallDetection", "Emergency SMS sent to $contactNumber")
        } catch (e: SecurityException) {
            Log.e("FallDetection", "Security error while sending SMS: ${e.message}")
        } catch (e: Exception) {
            Log.e("FallDetection", "Error while sending SMS: ${e.message}")
        }
    }

    private fun sendEmergencyEmailUsingJavaMail(emergencyEmail: String) {
        if (emergencyEmail.isEmpty()) {
            Log.e("FallDetection", "No email address configured.")
            return
        }

        val (username, password) = loadCredentials()
        Log.d("FallDetection", "Loaded credentials: username=$username")

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emergencyEmail)
                )
                subject = "Emergency: Fall Detected"
                setText("Warning: A possible fall has been detected. Location: $lastKnownLocation")
            }

            Thread {
                try {
                    Transport.send(message)
                    Log.d("FallDetection", "Emergency email sent to $emergencyEmail")
                } catch (e: MessagingException) {
                    Log.e("FallDetection", "Error sending email: ${e.message}")
                }
            }.start()

        } catch (e: MessagingException) {
            Log.e("FallDetection", "Error preparing email: ${e.message}")
        }
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (phoneNumber.isEmpty()) {
            Log.e("FallDetection", "No emergency number configured.")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                5
            )
            return
        }

        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        startActivity(callIntent)
    }

    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
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
    fun EmergencyContactInput(
        emergencyContact: String,
        emergencyEmail: String,
        onEmergencyContactChanged: (String) -> Unit,
        onEmergencyEmailChanged: (String) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Emergency Contact Number", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = emergencyContact,
                onValueChange = { onEmergencyContactChanged(it) },
                label = { Text("Enter emergency number") },
                placeholder = { Text("+391234567891") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Emergency Email", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = emergencyEmail,
                onValueChange = { onEmergencyEmailChanged(it) },
                label = { Text("Enter emergency email") },
                placeholder = { Text("example@email.com") },
                modifier = Modifier.fillMaxWidth()
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
                            text = "FALL DETECTED!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Is it a false alarm?",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onFalseAlarmClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "Yes, it's a false alarm",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = "Accelerometer Values", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "X: $x", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Y: $y", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Z: $z", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Acceleration: $totalAcceleration m/s²",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isFalling) {
                Text(text = "Fall Time: $fallTimestamp", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Location: $lastKnownLocation",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AccelerometerDisplayPreview() {
        FallDetectionTheme {
            Column {
                Header()
                EmergencyContactInput(
                    emergencyContact = "+391234567891",
                    emergencyEmail = "example@email.com",
                    onEmergencyContactChanged = {},
                    onEmergencyEmailChanged = {}
                )
                AccelerometerDisplay(
                    x = 0.0f,
                    y = 0.0f,
                    z = 0.0f,
                    totalAcceleration = 0.0f,
                    isFalling = true,
                    fallTimestamp = "15:30:00",
                    lastKnownLocation = "Latitude: 45.0, Longitude: 9.0",
                    onFalseAlarmClicked = {}
                )
            }
        }
    }
}