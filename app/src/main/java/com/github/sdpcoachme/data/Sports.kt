package com.github.sdpcoachme.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Sports(val sportName: String, val sportIcon: ImageVector) {
    SKI("Ski", Icons.Default.DownhillSkiing),
    TENNIS("Tennis", Icons.Default.SportsTennis),
    RUNNING("Running", Icons.Default.DirectionsRun),
    SWIMMING("Swimming", Icons.Default.Pool),
    WORKOUT("Workout", Icons.Default.FitnessCenter),
}