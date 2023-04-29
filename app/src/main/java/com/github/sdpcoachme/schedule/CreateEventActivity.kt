package com.github.sdpcoachme.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import java.time.LocalDateTime

class CreateEventActivity : ComponentActivity() {

    private lateinit var database: Database
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()

        println("Email: $email")

        if (email.isEmpty()) {
            val errorMsg = "New event did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {

            setContent {
                CoachMeTheme {
                    Dashboard("New Event") {
                        NewEvent()
                    }
                }
            }
        }
    }
}

@Composable
fun NewEvent() {
    val startSheet = rememberSheetState()
    val endSheet = rememberSheetState()

    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(LocalDateTime.now()) }
    var end by remember { mutableStateOf(LocalDateTime.now()) }
    var color by remember { mutableStateOf(Color.Blue) }
    var description by remember { mutableStateOf("") }

    Column (
        modifier = Modifier
            .padding(start = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .height(56.dp)
        ) {
            Text(
                text = "Event Title: ",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .padding(start = 20.dp)
                    .height(56.dp)
                    .align(Alignment.CenterVertically)
            )
        }


        // Start date
        CalendarDialog(
            state = startSheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH,

            ),
            selection = CalendarSelection.Date {
                start = it.atStartOfDay()
            }
        )

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .height(56.dp)
        ){
            Text(
                text = "Start Date: ",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
            ClickableText(
                text = AnnotatedString(start.toLocalDate().toString()),
                onClick = { startSheet.show() },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )

        }


        // End date
        CalendarDialog(
            state = endSheet,
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true,
                style = CalendarStyle.MONTH,
            ),
            selection = CalendarSelection.Date {
                end = it.atStartOfDay().plusHours(1)
            }
        )

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .height(56.dp)
        ){
            Text(
                text = "End Date: ",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
            ClickableText(
                text = AnnotatedString(end.toLocalDate().toString()),
                onClick = { endSheet.show() },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )

        }
    }
}

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
