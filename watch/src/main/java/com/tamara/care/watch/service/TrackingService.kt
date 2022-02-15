package com.tamara.care.watch.service

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.minew.beaconplus.sdk.MTCentralManager
import com.minew.beaconplus.sdk.MTPeripheral
import com.tamara.care.watch.R
import com.tamara.care.watch.data.entity.WatchInfoRequestEntity
import com.tamara.care.watch.data.eventBus.EventServiceDie
import com.tamara.care.watch.data.eventBus.MessageEventBus
import com.tamara.care.watch.data.room.WatchInfoEntity
import com.tamara.care.watch.manager.ConnectionManager
import com.tamara.care.watch.manager.NotificationManager
import com.tamara.care.watch.manager.SharedPreferencesManager
import com.tamara.care.watch.repo.BeaconInfoRepo
import com.tamara.care.watch.repo.ParametersRepo
import com.tamara.care.watch.utils.BeepHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService(), SensorsDataService {

    companion object {
        var isServiceTracking = false
        val locationLiveData = MutableLiveData<Location>()
        val heartRateLiveData = MutableLiveData<String>()
        val temperatureLiveData = MutableLiveData<String>()
        val gyroscopeLiveData = MutableLiveData<String>()
        val accelerometerLiveData = MutableLiveData<String>()
        val beaconsLiveData = MutableLiveData<List<MTPeripheral>>()
        const val LOCATION_UPDATE_INTERVAL = 10000L
        const val FASTEST_LOCATION_INTERVAL = 10000L
        const val REQUEST_INTERVAL = 600000L
        const val REGISTER_LISTENER_INTERVAL = 580000L
        //        const val REGISTER_LISTENER_INTERVAL_DELAY = 20000L
        private const val NOTIFICATION_ID = 1

        const val PATTERN_SERVER = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
        const val LOG_TAG = "serviceLifecycle"
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private lateinit var currentNotification: Notification
    private var wakeLock: PowerManager.WakeLock? = null
    val handler = Handler(Looper.getMainLooper())
    val handlerRegisterListener = Handler(Looper.getMainLooper())
    //    val handlerUnregisterListener = Handler(Looper.getMainLooper())
//    val handlerBeat = Handler(Looper.getMainLooper())
    private lateinit var runnableCode: Runnable
    //    private lateinit var runnableCodeBeat: Runnable
    private lateinit var runnableRegisterListeners: Runnable
//    private lateinit var runnableUnregisterListeners: Runnable
//    var isTime: Boolean = false

    @Inject
    lateinit var sharedPreferencesManager: SharedPreferencesManager

    @Inject
    lateinit var connectionManager: ConnectionManager

    @Inject
    lateinit var beaconInfoRepo: BeaconInfoRepo

    @Inject
    lateinit var parametersRepo: ParametersRepo

    @Inject
    lateinit var beepHelper: BeepHelper

//    var repeatCount = 0

    @SuppressLint("VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        isServiceTracking = true
        Log.d(LOG_TAG, "onCreate")
        notificationManager = NotificationManager(this)
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        createNotification()
        trackLocation()
        trackSensors()
        trackBeacons()
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")
        isServiceTracking = true
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("WakelockTimeout")
    private fun startTimer() {
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Tamara:WakeTag"
        )
        wakeLock?.acquire()
        runnableCode = object : Runnable {
            override fun run() {
                try {
                    sendData()
                } finally {
                    handler.postDelayed(this, REQUEST_INTERVAL)
                }
            }
        }
        runnableRegisterListeners = object : Runnable {
            override fun run() {
                try {
                    trackSensors();
//                    Thread.sleep(REGISTER_LISTENER_INTERVAL_DELAY)
//                    untrackSensors()
                } finally {
                    handlerRegisterListener.postDelayed(this, REGISTER_LISTENER_INTERVAL)
                }
            }
        }
//        runnableUnregisterListeners = object : Runnable {
//            override fun run() {
//                try {
//                    untrackSensors();
//                    Thread.sleep(10000)
//                } finally {
//                    handlerUnregisterListener
//                        .postDelayed(this, REGISTER_LISTENER_INTERVAL + REGISTER_LISTENER_INTERVAL_DELAY)
//                }
//            }
//        }

        runnableCode.run()
//        runnableUnregisterListeners.run()
        runnableRegisterListeners.run()
    }

    fun stopService() {
        try {
            if (wakeLock != null && wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
        }
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat(PATTERN_SERVER, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun sendData() {
        val info = beaconsLiveData.value?.sortedByDescending {
            it.mMTFrameHandler.rssi
        }

        if (connectionManager.checkBluetooth()) {
            if (connectionManager.isWiFiConnected.value == true && info?.isNotEmpty() == true) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (!info.isNullOrEmpty()) {
                        beaconInfoRepo.getAllBeaconsAndSendBeaconInfo(
                            peripheralsList = info,
                            currentDateAndTime = getCurrentTimeString()
                        )
                    } else {
//                        beepHelper.beep(2000)
                    }
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val parametersList = beaconInfoRepo.getAllInfoFromTable()
                    var gyroscopeChanges = 0
                    parametersList.forEachIndexed { index, parameterItem ->
                        if (index < parametersList.lastIndex) {
                            val rotation = parametersList[index + 1].gyroscope.toFloat() - parameterItem.gyroscope.toFloat()
                            val rotationString = rotation.toString().replace("-", "")

                            if (rotationString.toFloat() >= 4) {
                                gyroscopeChanges += 1
                            }
                        }
                    }
                    if (gyroscopeChanges >= 0.5f) {
                        gyroscopeChanges /= 4
                    }
                    if (parametersList.isNotEmpty()) {
                        parametersRepo.sendParametersInfo(
                            arrayListOf(
                                WatchInfoRequestEntity(
                                    gyroscope = parametersList.last().gyroscope,
                                    heartRate = parametersList.last().heartRate,
                                    accelerometer = parametersList.last().accelerometer,
                                    temperature = parametersList.last().temperature,
                                    transmitter = parametersList.last().transmitter,
                                    dateTime = parametersList.last().dateTime,
                                    gyroChanges = gyroscopeChanges
                                )
                            )
                        )
                    }
                    beaconInfoRepo.clearTable()
                    connectionManager.disableBluetooth()
                }
//                if (repeatCount < 2) {
//                    repeatCount++
//                } else {
//                    repeatCount = 0
//                    stopSelf()
//                }
            } else {
                val wifi = getSystemService(WIFI_SERVICE) as WifiManager
                wifi.isWifiEnabled = true
            }
        } else {
            connectionManager.enableBluetooth()
        }
    }

    private fun trackBeacons() {
        val mtCentralManager = MTCentralManager.getInstance(this)
        mtCentralManager.setBluetoothChangedListener { }
        mtCentralManager.startScan()
        mtCentralManager.setMTCentralManagerListener { peripherals ->
            beaconsLiveData.value = peripherals
        }
    }

    private fun trackSensors() {
        getTemperature()
        getHeartBeat()
        getGyro()
    }

    private fun unTrackGyroscopeSensor() {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope != null) {
            sensorManager.unregisterListener(this, gyroscope)
        }
    }

    private fun unTrackHeartRateSensor() {
        val heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRate != null) {
            sensorManager.unregisterListener(this, heartRate)
        }
    }

    private fun unTrackTemperatureSensor() {
        val temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperature != null) {
            sensorManager.unregisterListener(this, temperature)
        }
    }

    private fun getGyro() {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun getHeartBeat() {
        val heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRate != null) {
            sensorManager.registerListener(this, heartRate, SensorManager.SENSOR_DELAY_NORMAL)
//            runnableCodeBeat = object : Runnable {
//                override fun run() {
//                    try {
//                        isTime = true
//                    } finally {
//                        handlerBeat.postDelayed(this, 1200000)
//                    }
//                }
//            }
//            runnableCodeBeat.run()
        }
    }

    private fun getTemperature() {
        val temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperature != null) {
            sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            locationLiveData.value = result.lastLocation
        }
    }

    @SuppressLint("MissingPermission")
    private fun trackLocation() {
        val request = LocationRequest().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = FASTEST_LOCATION_INTERVAL
            priority = PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createNotification() {
        notificationManager.createNotification(
            title = getString(R.string.service_state),
            description = "run",
        )
        currentNotification = notificationManager.getCurrentNotification()
        startForeground(NOTIFICATION_ID, currentNotification)
    }

    private fun createStopNotification() {
        stopForeground(true)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerLiveData.value = event.values[0].toString()
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                temperatureLiveData.value = event.values[0].toString()
                unTrackTemperatureSensor()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeLiveData.value = event.values[0].toString()
//                unTrackGyroscopeSensor()
                lifecycleScope.launch(Dispatchers.IO) {
                    beaconInfoRepo.saveData(
                        WatchInfoEntity(
                            heartRate = heartRateLiveData.value ?: "-1",
                            accelerometer = accelerometerLiveData.value,
                            temperature = temperatureLiveData.value,
                            gyroscope = gyroscopeLiveData.value ?: "-1",
                            transmitter = sharedPreferencesManager.transmitterId,
                            dateTime = getCurrentTimeString()
                        )
                    )
                }
            }
            Sensor.TYPE_HEART_RATE -> {
                heartRateLiveData.value = event.values[0].toString()
                if (event.values[0] > 0) {
                    unTrackHeartRateSensor()
                }
//                if (isTime) {
//                    heartRateLiveData.value = event.values[0].toString()
//                    isTime = false;
//                }
            }
            else -> {

            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        isServiceTracking = false
        handler.removeCallbacks(runnableCode)
        handlerRegisterListener.removeCallbacks(runnableRegisterListeners)
//        handlerUnregisterListener.removeCallbacks(runnableUnregisterListeners)
//        handlerBeat.removeCallbacks(runnableCodeBeat)
        sensorManager.unregisterListener(this)
        MessageEventBus.send(EventServiceDie())
        MTCentralManager.getInstance(this).stopScan()
    }

}