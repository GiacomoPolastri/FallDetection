package com.example.falldetection.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorHandler(context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    init {
        // Inizializzazione dei sensori
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun setupAccelerometer() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun setupGyroscope() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun startSensorMonitoring() {
        setupAccelerometer()
        setupGyroscope()
    }

    fun stopSensorMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(it.values)
                Sensor.TYPE_GYROSCOPE -> handleGyroscopeData(it.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non implementato, ma potrebbe essere usato per gestire cambiamenti nella precisione dei sensori
    }

    private fun handleAccelerometerData(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        Log.d("SensorHandler", "Accelerometer Data: x=$x, y=$y, z=$z")
        // Logica per rilevare la caduta usando i dati dell'accelerometro
    }

    private fun handleGyroscopeData(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        Log.d("SensorHandler", "Gyroscope Data: x=$x, y=$y, z=$z")
        // Logica per rilevare movimenti anomali usando i dati del giroscopio
    }
}