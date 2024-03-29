package com.github.sdpcoachme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

// Adding extra colors for messages and other things

// For rating stars
@get:Composable
val Colors.selectedStar: Color
    get() = Gold
@get:Composable
val Colors.unselectedStar: Color
    get() = if(isLight) LightGray else DarkGray

@get:Composable
val Colors.rating: Color
    get() = Gold
@get:Composable
val Colors.onRating: Color
    get() = White

// Label for group event items
@get:Composable
val Colors.label: Color
    get() = DarkOrange
@get:Composable
val Colors.onLabel: Color
    get() = White

// Label for chat
@get:Composable
val Colors.messageMe: Color
    get() = if (isLight) Purple100 else Purple800 // or Purple900 ?
@get:Composable
val Colors.messageOther: Color
    get() = if (isLight) LightGray else DarkDarkGray
@get:Composable
val Colors.onMessage: Color
    get() = if (isLight) Color.Black else White
@get:Composable
val Colors.onMessageTimeStamp: Color
    get() = if (isLight) DarkGray else LightGray
@get:Composable
val Colors.chatTime: Color
    get() = if (isLight) LightLightGray else DarkGray
@get:Composable
val Colors.onChatTime: Color
    get() = if (isLight) Color.Black else White
@get:Composable
val Colors.readMessageCheck: Color
    get() = if (isLight) DarkPrettyBlue else LightPrettyBlue


@Composable
fun CoachMeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}


// Create material 3 theme to make sure the sheets from maxkeppeler pick up the dark mode
// TODO: uncomment this to enable dark mode for maxkeppeler sheets
//val AppDarkMaterial3ColorScheme = darkColorScheme()
//val AppLightMaterial3ColorScheme = lightColorScheme()
//
//val Material3Typography = androidx.compose.material3.Typography()
//
//val Material3Shapes = androidx.compose.material3.Shapes()
//
//@Composable
//fun CoachMeMaterial3Theme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
//    val appColorScheme = if (darkTheme) {
//        AppDarkMaterial3ColorScheme
//    } else {
//        AppLightMaterial3ColorScheme
//    }
//
//    androidx.compose.material3.MaterialTheme(
//        colorScheme = appColorScheme,
//        typography = Material3Typography,
//        shapes = Material3Shapes,
//        content = content
//    )
//}