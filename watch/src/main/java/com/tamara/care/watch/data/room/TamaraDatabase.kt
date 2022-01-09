package com.tamara.care.watch.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchInfoEntity::class], version = 2)
abstract class TamaraDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME: String = "tamara_db"
    }

    abstract fun watchDao(): WatchInfoDao
}