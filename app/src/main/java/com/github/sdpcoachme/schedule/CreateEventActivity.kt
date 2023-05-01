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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Scaffold
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.Purple500
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import com.maxkeppeler.sheets.clock.ClockDialog
import com.maxkeppeler.sheets.clock.models.ClockConfig
import com.maxkeppeler.sheets.clock.models.ClockSelection
import com.maxkeppeler.sheets.color.ColorDialog
import com.maxkeppeler.sheets.color.models.ColorSelection
import com.maxkeppeler.sheets.color.models.SingleColor
import java.time.LocalDateTime

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
    val formatter = EventOps.getEventTimeFormatter()
    val context = LocalContext.current

    var eventName by remember { mutableStateOf("Test Event") }
    var start by remember { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0)) }
    var end by remember { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)) }
    var color by remember { mutableStateOf(SingleColor(Color.Blue.toArgb(), null, null)) }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            fun goBackToScheduleActivity() {
                val intent = Intent(context, ScheduleActivity::class.java)
                println("Email in create activity database: ${database.getCurrentEmail()}")
                intent.putExtra("email", database.getCurrentEmail())
                context.startActivity(intent)
            }
            val event = Event(eventName, color.colorInt.toString(), start.format(formatter), end.format(formatter), description)
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
                            EventOps.addEvent(event, database)
                            println("add event returned")
                            goBackToScheduleActivity()
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
                .padding(start = 10.dp, end = 10.dp),
            //verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 10.dp),
            ) {
                TextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                )
            }

            // Start date
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
                        start = it.atStartOfDay()
                    }
                )
                ClickableText(
                    text = AnnotatedString(start.toLocalDate().toString()),
                    onClick = { startDateSheet.show() },
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(.8f)
                )
            }

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
                        start = start.withHour(hours).withMinute(minutes)
                    }
                )
                ClickableText(
                    text = AnnotatedString(start.toLocalTime().format(EventOps.getEventTimeFormatter())),
                    onClick = { startTimeSheet.show() },
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(.8f)
                )
            }

            // End date
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
                        end = it.atStartOfDay().plusHours(1)
                    }
                )
                ClickableText(
                    text = AnnotatedString(end.toLocalDate().toString()),
                    onClick = { endDateSheet.show() },
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(.8f)
                )
            }

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
                        end = end.withHour(hours).withMinute(minutes)
                    }
                )
                ClickableText(
                    text = AnnotatedString(end.toLocalTime().format(EventOps.getEventTimeFormatter())),
                    onClick = { endTimeSheet.show() },
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(.8f)
                )
            }

            Row (
                modifier = Modifier
                    //.fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Color: ",
                    modifier = Modifier
                        .weight(1f)
                )
                ColorDialog(
                    state = colorSheet,
                    selection = ColorSelection(
                        selectedColor = color,
                        onSelectColor = { color = SingleColor(it) }
                    )
                )
                Box (
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(.5f)
                        .aspectRatio(1f, true)
                        .size(5.dp)
                        .clickable { colorSheet.show() }
                        .background(Color(color.colorInt!!))
                )
            }

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.Bottom
            ){
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun AddEventTitleRow(database: Database, event: Event) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Purple500)
    ) {
        fun goBackToScheduleActivity() {
            val intent = Intent(context, ScheduleActivity::class.java)
            context.startActivity(intent)
        }

        // Button icon for the cancel button
        IconButton(
            onClick = {
                goBackToScheduleActivity()
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 5.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Cancel",
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Button icon for the save button
        IconButton(
            onClick = {
                EventOps.addEvent(event, database)
                goBackToScheduleActivity()
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 5.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Save",
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = Color.White
            )
        }
    }
}

// TODO: Use that to modularize the date rows
/*@Composable
fun DateRow(startOrEnd: String, sheet: SheetState, date: LocalDateTime, setDate: (LocalDateTime) -> Unit) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.Bottom
    ){
        Text(
            text = "$startOrEnd Date: ",
            modifier = Modifier
                .weight(1f)

        )
        CalendarDialog(
            state = sheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH,

                ),
            selection = CalendarSelection.Date { date ->
                setDate(date.atStartOfDay())
            }
        )
        ClickableText(
            text = AnnotatedString(date.toLocalDate().toString()),
            onClick = { sheet.show() },
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .weight(1f)
        )
    }
}*/

/*
@Composable
fun NewEventTitleRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Purple500)
    ) {
        // Button icon for the back button
        IconButton(
            onClick = {
                // go back to the map view
                val intent = Intent(context, MapActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .testTag(ScheduleActivity.TestTags.Buttons.BACK)
                .padding(start = 5.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = Color.White
            )
        }
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 10.dp)
        )
    }
}*/