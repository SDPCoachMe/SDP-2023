package com.github.sdpcoachme.data.schedule

import androidx.compose.ui.graphics.Color

enum class EventColors(val color: Color) {
    RED(Color(0xFFD56767)),
    SALMON(Color(0xFFE79484)),
    ORANGE(Color(0xFFDF9E5D)),
    LIME(Color(0xFFA4DC7A)),
    MINT(Color(0xFF7ADCC0)),
    DARK_GREEN(Color(0xFF83AF83)),
    BLUE(Color(0xFF6290D5)),
    LIGHT_BLUE(Color(0xFF7AC2DC)),
    PURPLE(Color(0xFFBF92E2)),
    DEFAULT(RED.color)
}