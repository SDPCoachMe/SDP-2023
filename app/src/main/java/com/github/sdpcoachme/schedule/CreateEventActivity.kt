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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.maxkeppeker.sheets.core.models.base.Header
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
import com.maxkeppeler.sheets.color.models.ColorSelectionMode
import com.maxkeppeler.sheets.color.models.MultipleColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CreateEventActivity : ComponentActivity() {
    class TestTags {
        class Texts {
            companion object {
                fun text(tag: String): String {
                    return "${tag}Text"
                }

                val ACTIVITY_TITLE = text("activityTitle")
                val START_DATE_TEXT = text("startDate")
                val START_TIME_TEXT = text("startTime")
                val END_DATE_TEXT = text("endDate")
                val END_TIME_TEXT = text("endTime")
                val COLOR_TEXT = text("color")

                val START_DATE_DIALOG_TITLE = text("startDateDialogTitle")
                val END_DATE_DIALOG_TITLE = text("endDateDialogTitle")
                val START_TIME_DIALOG_TITLE = text("startTimeDialogTitle")
                val END_TIME_DIALOG_TITLE = text("endTimeDialogTitle")
                val COLOR_DIALOG_TITLE = text("colorDialogTitle")
            }
        }

        class Clickables {
            companion object {
                private fun clickableText(tag: String): String {
                    return "${tag}ClickableText"
                }

                private fun button(tag: String): String {
                    return "${tag}Button"
                }

                private fun box(tag: String): String {
                    return "${tag}Box"
                }

                val START_DATE = clickableText("startDate")
                val START_TIME = clickableText("startTime")
                val END_DATE = clickableText("endDate")
                val END_TIME = clickableText("endTime")
                val SAVE = button("save")
                val CANCEL = button("cancel")
                val COLOR_BOX = box("color")
            }
        }

        class TextFields {
            companion object {
                fun textField(tag: String): String {
                    return "${tag}TextField"
                }

                val EVENT_NAME = textField("eventName")
                val DESCRIPTION = textField("description")
            }
        }

        class Icons {
            companion object {
                fun icon(tag: String): String {
                    return "${tag}Icon"
                }

                val SAVE_ICON = icon("save")
                val CANCEL_ICON = icon("cancel")
            }
        }


        companion object {
            const val SCAFFOLD = "scaffold"
        }
    }

    private lateinit var database: Database
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()
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
    var start by remember { mutableStateOf(EventOps.getDefaultEventStart()) }
    var end by remember { mutableStateOf(EventOps.getDefaultEventEnd()) }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(EventColors.DEFAULT.color) }

    Scaffold(
        modifier = Modifier.testTag(CreateEventActivity.TestTags.SCAFFOLD),
        topBar = {
            fun goBackToScheduleActivity() {
                val intent = Intent(context, ScheduleActivity::class.java)
                context.startActivity(intent)
            }
            val event = Event(eventName, selectedColor.value.toString(), start.format(formatterEventDate), end.format(formatterEventDate), description)
            TopAppBar(
                title = {
                    Text(
                        text = "New Event",
                        modifier = Modifier.testTag(CreateEventActivity.TestTags.Texts.ACTIVITY_TITLE)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { goBackToScheduleActivity() },
                        modifier = Modifier.testTag(CreateEventActivity.TestTags.Clickables.CANCEL),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.testTag(CreateEventActivity.TestTags.Icons.CANCEL_ICON),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            EventOps.addEvent(event, database).thenAccept {
                                goBackToScheduleActivity()
                            }
                        },
                        modifier = Modifier.testTag(CreateEventActivity.TestTags.Clickables.SAVE),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done",
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.testTag(CreateEventActivity.TestTags.Icons.SAVE_ICON),
                        )
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
                    .testTag(CreateEventActivity.TestTags.TextFields.EVENT_NAME)
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

@OptIn(ExperimentalComposeUiApi::class)
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
                .testTag(CreateEventActivity.TestTags.Texts.START_DATE_TEXT)
        )
        CalendarDialog(
            state = startDateSheet,
            header = Header.Custom {
                Text(
                    text = "Start Date",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CreateEventActivity.TestTags.Texts.START_DATE_DIALOG_TITLE)
                )
            },
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH),
            selection = CalendarSelection.Date {
                onDateChange(it.atStartOfDay())
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true,
            )
        )
        ClickableText(
            text = AnnotatedString(start.format(formatter)),
            onClick = { startDateSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
                .testTag(CreateEventActivity.TestTags.Clickables.START_DATE)
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
                .testTag(CreateEventActivity.TestTags.Texts.START_TIME_TEXT)
        )
        ClockDialog(
            state = startTimeSheet,
            header = Header.Custom {
                Text(
                    text = "Start Time",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CreateEventActivity.TestTags.Texts.START_TIME_DIALOG_TITLE)
                )
            },
            config = ClockConfig(
                defaultTime = start.toLocalTime(),
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
                .testTag(CreateEventActivity.TestTags.Clickables.START_TIME)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
                .testTag(CreateEventActivity.TestTags.Texts.END_DATE_TEXT)
        )
        CalendarDialog(
            header = Header.Custom {
                Text(
                    text = "End Date",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CreateEventActivity.TestTags.Texts.END_DATE_DIALOG_TITLE)
                )
            },
            state = endDateSheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH,
            ),
            selection = CalendarSelection.Date {
                onDateChange(it.atStartOfDay().plusHours(1))
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true,
            )
        )
        ClickableText(
            text = AnnotatedString(end.format(formatter)),
            onClick = { endDateSheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(.8f)
                .testTag(CreateEventActivity.TestTags.Clickables.END_DATE)
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
                .testTag(CreateEventActivity.TestTags.Texts.END_TIME_TEXT)
        )
        ClockDialog(
            state = endTimeSheet,
            header = Header.Custom {
                Text(
                    text = "End Time",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CreateEventActivity.TestTags.Texts.END_TIME_DIALOG_TITLE)
                )
            },
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
                .testTag(CreateEventActivity.TestTags.Clickables.END_TIME)
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
                .testTag(CreateEventActivity.TestTags.Texts.COLOR_TEXT)
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
            header = Header.Custom {
                Text(
                    text = "Select color",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CreateEventActivity.TestTags.Texts.COLOR_DIALOG_TITLE)
                )
            },
            selection = ColorSelection(
                onSelectColor = {
                    onColorChange(colorMap[it] ?: EventColors.DEFAULT.color)
                }
            ),
            config = ColorConfig(
                displayMode = ColorSelectionMode.TEMPLATE,
                templateColors = MultipleColors.ColorsInt(colorMap.keys.toList().dropLast(1)), //all except default
                allowCustomColorAlphaValues = false,
            )
        )
        Box (
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(.5f)
                .aspectRatio(1f, true)
                .size(5.dp)
                .clickable { colorSheet.show() }
                .background(selectedColor)
                .testTag(CreateEventActivity.TestTags.Clickables.COLOR_BOX)
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
        val focusManager = LocalFocusManager.current
        TextField(
            value = description,
            onValueChange = { onDescriptionChange(it) },
            label = { Text("Description") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus() }
            ),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag(CreateEventActivity.TestTags.TextFields.DESCRIPTION)
        )
    }
}

