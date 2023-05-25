package com.github.sdpcoachme.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Sports(val sportName: String, val sportIcon: ImageVector) {
    BASKETBALL("Basketball", Icons.Default.SportsBasketball),
    CLIMBING("Climbing", Icons.Default.Terrain),
    CYCLING("Cycling", Icons.Default.DirectionsBike),
    FOOTBALL("Football", Icons.Default.SportsSoccer),
    GOLF("Golf", Icons.Default.GolfCourse),
    GYMNASTICS("Gymnastics", Icons.Default.SportsGymnastics),
    HIKING("Hiking", Icons.Default.Hiking),
    HOCKEY("Hockey", Icons.Default.SportsHockey),
    KAYAKING("Kayaking", Icons.Default.Kayaking),
    ROWING("Rowing", Icons.Default.Rowing),
    RUNNING("Running", Icons.Default.DirectionsRun),
    SKI("Ski", Icons.Default.DownhillSkiing),
    SNOWBOARD("Snowboard", Icons.Default.Snowboarding),
    SURFING("Surfing", Icons.Default.Surfing),
    SWIMMING("Swimming", Icons.Default.Pool),
    TENNIS("Tennis", Icons.Default.SportsTennis),
    VOLLEYBALL("Volleyball", Icons.Default.SportsVolleyball),
    WORKOUT("Workout", Icons.Default.FitnessCenter),
    YOGA("Yoga", Icons.Default.SelfImprovement),
    WATERSKI("Waterski", Icons.Default.DownhillSkiing),
}