package com.github.sdpcoachme

import android.app.Application
import com.github.sdpcoachme.container.AppContainer

// Base class for maintaining global application state.
class CoachMeApplication : Application() {
    val appContainer = AppContainer()
}