package com.github.sdpcoachme.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.maxkeppeker.sheets.core.models.base.SheetState
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import com.maxkeppeler.sheets.clock.ClockDialog
import com.maxkeppeler.sheets.clock.models.ClockConfig
import com.maxkeppeler.sheets.clock.models.ClockSelection
import com.maxkeppeler.sheets.color.ColorDialog
import com.maxkeppeler.sheets.color.models.ColorConfig
import com.maxkeppeler.sheets.color.models.ColorSelection
import com.maxkeppeler.sheets.color.models.MultipleColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CreateEventActivity : ComponentActivity() {

    private lateinit var database: Database
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = intent.getStringExtra("email") ?: ""

        if (email.isEmpty()) {
            val errorMsg = "New event did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            setContent {
                CoachMeTheme {
                    Surface(color = MaterialTheme.colors.background) {
                        NewEvent(database)
                    }
                }
            }
        }
    }
}


@Composable
fun NewEvent(database: Database) {
    val startDateSheet = rememberSheetState()
    val startTimeSheet = rememberSheetState()
    val endDateSheet = rememberSheetState()
    val endTimeSheet = rememberSheetState()
    val colorSheet = rememberSheetState()
    val context = LocalContext.current
    val formatterEventDate = EventOps.getEventDateFormatter()
    val formatterUserDate = EventOps.getDayFormatter()
    val formatterUserTime = EventOps.getTimeFormatter()

    var eventName by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0)) }
    var end by remember { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)) }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(EventColors.RED.color) }

    Scaffold(
        topBar = {
            fun goBackToScheduleActivity() {
                val intent = Intent(context, ScheduleActivity::class.java)
                context.startActivity(intent)
            }
            val event = Event(eventName, selectedColor.value.toString(), start.format(formatterEventDate), end.format(formatterEventDate), description)
            TopAppBar(
                title = {
                    Text("New Event")
                },
                navigationIcon = {
                    IconButton(onClick = { goBackToScheduleActivity() }) {
                        Icon(Icons.Filled.Cancel, "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            EventOps.addEvent(event, database).thenAccept {
                                goBackToScheduleActivity()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Done, "Done",
                            tint = MaterialTheme.colors.onPrimary)
                    }
                })
        }
    ) {padding ->
        Column (
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
        ) {
            val focusManager = LocalFocusManager.current
            TextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Event Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )

            // Start date
            StartDateRow(
                startDateSheet = startDateSheet,
                start = start,
                formatter = formatterUserDate,
                onDateChange = { start = it },
            )
            StartTimeRow(
                startTimeSheet = startTimeSheet,
                start = start,
                formatter = formatterUserTime,
                onTimeChange = { start = it }
            )

            // End date
            EndDateRow(
                endDateSheet = endDateSheet,
                end = end,
                formatter = formatterUserDate,
                onDateChange = { end = it },
            )
            EndTimeRow(
                endTimeSheet = endTimeSheet,
                end = end,
                formatter = formatterUserTime,
                onTimeChange = { end = it }
            )

            // Color
            ColorRow(
                colorSheet = colorSheet,
                selectedColor = selectedColor,
                onColorChange = { selectedColor = it }
            )

            // Description
            DescriptionRow(
                description = description,
                onDescriptionChange = { description = it }
            )
        }
    }
}

@Composable
fun StartDateRow(
    startDateSheet: SheetState,
    start: LocalDateTime,
    formatter: DateTimeFormatter,
    onDateChange: (LocalDateTime) -> Unit
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ){
        Text(
            text = "Start Date: ",
            modifier = Modifier
                .weight(1f)
        )
        CalendarDialog(
            state = startDateSheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH),
            selection = CalendarSelection.Date {
                onDateChange(it.atStartOfDay())
            }
        )
        ClickableText(
            text = AnnotatedString(start.format(formatter)),
            onClick = { startDateSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
        )
    }
}

@Composable
fun StartTimeRow(
    startTimeSheet: SheetState,
    start: LocalDateTime,
    formatter: DateTimeFormatter,
    onTimeChange: (LocalDateTime) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Time: ",
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
        )
        ClockDialog(
            state = startTimeSheet,
            config = ClockConfig(
                is24HourFormat = true
            ),
            selection = ClockSelection.HoursMinutes { hours, minutes ->
                onTimeChange(start.withHour(hours).withMinute(minutes))
            }
        )
        ClickableText(
            text = AnnotatedString(start.toLocalTime().format(formatter)),
            onClick = { startTimeSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
        )
    }
}

@Composable
fun EndDateRow(
    endDateSheet: SheetState,
    end: LocalDateTime,
    formatter: DateTimeFormatter,
    onDateChange: (LocalDateTime) -> Unit
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ){
        Text(
            text = "End Date: ",
            modifier = Modifier
                .weight(1f)
        )
        CalendarDialog(
            state = endDateSheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH,
            ),
            selection = CalendarSelection.Date {
                onDateChange(it.atStartOfDay().plusHours(1))
            }
        )
        ClickableText(
            text = AnnotatedString(end.format(formatter)),
            onClick = { endDateSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
        )
    }
}

@Composable
fun EndTimeRow(
    endTimeSheet: SheetState,
    end: LocalDateTime,
    formatter: DateTimeFormatter,
    onTimeChange: (LocalDateTime) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Time: ",
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
        )
        ClockDialog(
            state = endTimeSheet,
            config = ClockConfig(
                is24HourFormat = true
            ),
            selection = ClockSelection.HoursMinutes { hours, minutes ->
                onTimeChange(end.withHour(hours).withMinute(minutes))
            }
        )
        ClickableText(
            text = AnnotatedString(end.toLocalTime().format(formatter)),
            onClick = { endTimeSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
        )
    }
}

@Composable
fun ColorRow(
    colorSheet: SheetState,
    selectedColor: Color,
    onColorChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Color: ",
            modifier = Modifier
                .weight(1f)
        )

        val colorMap = mapOf(
            EventColors.RED.color.toArgb() to EventColors.RED.color,
            EventColors.SALMON.color.toArgb() to EventColors.SALMON.color,
            EventColors.ORANGE.color.toArgb() to EventColors.ORANGE.color,
            EventColors.LIME.color.toArgb() to EventColors.LIME.color,
            EventColors.MINT.color.toArgb() to EventColors.MINT.color,
            EventColors.DARK_GREEN.color.toArgb() to EventColors.DARK_GREEN.color,
            EventColors.BLUE.color.toArgb() to EventColors.BLUE.color,
            EventColors.LIGHT_BLUE.color.toArgb() to EventColors.LIGHT_BLUE.color,
            EventColors.PURPLE.color.toArgb() to EventColors.PURPLE.color,
        )

        ColorDialog(
            state = colorSheet,
            selection = ColorSelection(
                onSelectColor = {
                    onColorChange(colorMap[it] ?: EventColors.RED.color)
                }
            ),
            config = ColorConfig(templateColors = MultipleColors.ColorsInt(colorMap.keys.toList()))
        )
        Box (
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(.5f)
                .aspectRatio(1f, true)
                .size(5.dp)
                .clickable { colorSheet.show() }
                .background(selectedColor)
        )
    }
}

@Composable
fun DescriptionRow(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Row (modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.Bottom
    ){
        TextField(
            value = description,
            onValueChange = { onDescriptionChange(it) },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}

