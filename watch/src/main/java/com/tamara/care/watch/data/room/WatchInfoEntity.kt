package com.tamara.care.watch.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchInfo")
data class WatchInfoEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "gyroscope")
    val gyroscope: String,

    @ColumnInfo(name = "heartRate")
    val heartRate: String,

    @ColumnInfo(name = "accelerometer")
    val accelerometer: String?,

    @ColumnInfo(name = "temperature")
    val temperature: String?,

    @ColumnInfo(name = "transmitter")
    val transmitter: String?,

    @ColumnInfo(name = "dateTime")
    val dateTime: String?
)
