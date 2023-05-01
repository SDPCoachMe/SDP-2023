package com.github.sdpcoachme

import androidx.compose.ui.graphics.Color
import com.maxkeppeler.sheets.color.models.SingleColor
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun colorTest() {
//        val color = SingleColor(0xFF0000FF.toInt())
//        val colorString = color.colorInt.toString()
        val colorString = "18446744073692774655"

        val number = colorString.toULong()
        val result = Color(number)

        println("result: ${result.value}")
    }
}