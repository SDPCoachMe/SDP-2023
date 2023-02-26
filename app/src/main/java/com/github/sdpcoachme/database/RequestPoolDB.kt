package com.github.sdpcoachme.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RequestPoolDB {
    @Query("select * from LineDB")
    fun getAll(): List<LineDB>

    @Insert
    fun insert(vararg lineDB: LineDB)

    @Query("DELETE FROM lineDB")
    fun deleteDB()
}