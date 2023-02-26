package com.github.sdpcoachme.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LineDB::class], version = 1)
abstract class AppDB : RoomDatabase() {
    abstract fun userDB(): RequestPoolDB
}