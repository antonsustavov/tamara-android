package com.tamara.care.watch.data.entity

data class WatchInfoRequestEntity(
    val gyroscope: String,
    val heartRate: String,
    val accelerometer: String?,
    val temperature: String?,
    val transmitter: String?,
    val dateTime: String?,
    val gyroChanges: Int?,
)
