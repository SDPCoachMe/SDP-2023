package com.github.sdpcoachme.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LineDB(
        @PrimaryKey val uid: Int,
        @ColumnInfo(name = "activity") val activity : String?,
        @ColumnInfo(name = "type") val type : String?,
        @ColumnInfo(name = "participants") val participants : Int?,
        @ColumnInfo(name = "price") val price : Double?,
        @ColumnInfo(name = "link") val link : String?,
        @ColumnInfo(name = "key") val key : Int?,
        @ColumnInfo(name = "accessibility") val accessibility : Double?
)
