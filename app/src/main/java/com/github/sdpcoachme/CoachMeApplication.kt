package com.github.sdpcoachme

import android.app.Application
import com.github.sdpcoachme.data.Database
import com.github.sdpcoachme.data.RealDatabase

open class CoachMeApplication : Application() {
    open val database: Database = RealDatabase()
}