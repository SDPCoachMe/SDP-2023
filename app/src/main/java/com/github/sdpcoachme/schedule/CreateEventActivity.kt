@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.sdpcoachme.schedule

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.data.schedule.EventType
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.autocomplete.AddressAutocompleteHandler
import com.github.sdpcoachme.profile.SelectSportsActivity
import com.github.sdpcoachme.ui.theme.CoachMeMaterial3Theme
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
import java.util.concurrent.CompletableFuture

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
                val MAX_PARTICIPANTS_TEXT = text("maxParticipants")
                val SPORT_TEXT = text("sport")
                val LOCATION_TEXT = text("location")
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
                val SPORT = clickableText("sport")
                val LOCATION = clickableText("location")
                val SAVE = button("save")
                val CANCEL = button("cancel")
                val COLOR_BOX = box("color")
            }
        }

        class TextFields {
            companion object {
                private fun textField(tag: String): String {
                    return "${tag}TextField"
                }

                val EVENT_NAME = textField("eventName")
                val MAX_PARTICIPANTS = textField("maxParticipants")
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

        class SportElement(sport: Sports) {
            val ICON = "${sport.sportName}Icon"
        }


        companion object {
            const val SCAFFOLD = "scaffold"
        }
    }

    private lateinit var store: CachingStore
    private lateinit var addressAutocompleteHandler: AddressAutocompleteHandler
    private lateinit var selectSportsHandler: (Intent) -> CompletableFuture<List<Sports>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store

        // Set up handler for calls to location autocomplete
        addressAutocompleteHandler = (application as CoachMeApplication).addressAutocompleteHandler(this, this)

        // Set up handler for calls to select sports activity
        selectSportsHandler = SelectSportsActivity.getHandler(this)

        val eventTypeName = intent.getStringExtra("eventType")!!
        val eventType = EventType.fromString(eventTypeName)!!
        setContent {
            CoachMeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    NewEvent(eventType)
                }
            }
        }
    }

    @Composable
    fun NewEvent(eventType: EventType) {
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
        var maxParticipants by remember { mutableStateOf(0) }
        var sports by remember { mutableStateOf(listOf(Sports.RUNNING)) }
        var location by remember {
            mutableStateOf(Address())
        }
        var selectedColor by remember { mutableStateOf(EventColors.DEFAULT.color) }
        var description by remember { mutableStateOf("") }

        Scaffold(
            modifier = Modifier.testTag(TestTags.SCAFFOLD),
            topBar = {
                val event = Event(
                    name = eventName,
                    color = selectedColor.value.toString(),
                    start = start.format(formatterEventDate),
                    end = end.format(formatterEventDate),
                    address = location,   // TODO: Add possibility to choose location during next task
                    description = description
                )
                TopAppBar(
                    title = {
                        Text(
                            text = "New Event",
                            modifier = Modifier.testTag(TestTags.Texts.ACTIVITY_TITLE)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                finish() // Go back to previous activity (removes itself from activity stack)
                            },
                            modifier = Modifier.testTag(TestTags.Clickables.CANCEL),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Cancel",
                                modifier = Modifier.testTag(TestTags.Icons.CANCEL_ICON),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (start.isAfter(end)) {
                                    val toast = Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_SHORT)
                                    toast.show()
                                } else {
                                    if (eventType == EventType.PRIVATE) {
                                        EventOps.addEvent(event, store).thenAccept {
                                            finish() // Go back to previous activity (removes itself from activity stack)
                                        }
                                    } else if (eventType == EventType.GROUP) {
                                        // TODO: temporary, until we stop considering the organiser as a participant
                                        //  an event must have at least 2 participants, since for now the organiser is
                                        //  considered as a participant, and we don't want to have an event with only
                                        //  the organiser as a participant
                                        if (maxParticipants <= 2) {
                                            val toast = Toast.makeText(context, "Max participants must be greater than 2", Toast.LENGTH_SHORT)
                                            toast.show()
                                        } else if (start.isBefore(LocalDateTime.now())) {
                                            val toast = Toast.makeText(context, "You can't create a group event in the past", Toast.LENGTH_SHORT)
                                            toast.show()
                                        } else {
                                            store.getCurrentEmail().thenCompose { organiser ->
                                                val groupEvent = GroupEvent(
                                                    event = event,
                                                    organizer = organiser,
                                                    maxParticipants = maxParticipants,
                                                    participants = listOf(),
                                                )
                                                EventOps.addGroupEvent(groupEvent, store).thenCompose {
                                                    // TODO: temporary since we consider organiser as a participant
                                                    store.registerForGroupEvent(groupEventId = groupEvent.groupEventId)
                                                }
                                            }.thenAccept {
                                                finish() // Go back to previous activity (removes itself from activity stack)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag(TestTags.Clickables.SAVE),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done",
                                tint = if (MaterialTheme.colors.isLight)
                                    MaterialTheme.colors.onPrimary
                                else
                                    MaterialTheme.colors.onSurface,
                                modifier = Modifier.testTag(TestTags.Icons.SAVE_ICON),
                            )
                        }
                    })
            }
        ) { padding ->
            Surface(
                color = MaterialTheme.colors.background,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CoachMeMaterial3Theme {
                    Column (
                        modifier = Modifier.padding(horizontal = 10.dp)
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
                                .testTag(TestTags.TextFields.EVENT_NAME)
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

                        if (eventType == EventType.GROUP) {
                            MaxParticipantsRow(
                                maxParticipants = maxParticipants,
                                onMaxParticipantsChange = { maxParticipants = it }
                            )
                        }

                        EventSportRow(
                            sport = sports,
                            onSportChange = { sports = it }
                        )

                        EventLocationRow(
                            location = location,
                            onLocationChange = { location = it }
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
                    .testTag(TestTags.Texts.START_DATE_TEXT)
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
                            .testTag(TestTags.Texts.START_DATE_DIALOG_TITLE)
                    )
                },
                config = CalendarConfig(
                    monthSelection = false,
                    yearSelection = false,
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
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.Clickables.START_DATE)
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
                    .testTag(TestTags.Texts.START_TIME_TEXT)
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
                            .testTag(TestTags.Texts.START_TIME_DIALOG_TITLE)
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
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.Clickables.START_TIME)
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
                    .testTag(TestTags.Texts.END_DATE_TEXT)
            )
            CalendarDialog(
                header = Header.Custom {
                    Text(
                        text = "End Date",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.END_DATE_DIALOG_TITLE)
                    )
                },
                state = endDateSheet,
                config = CalendarConfig(
                    monthSelection = false,
                    yearSelection = false,
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
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.Clickables.END_DATE)
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
                    .testTag(TestTags.Texts.END_TIME_TEXT)
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
                            .testTag(TestTags.Texts.END_TIME_DIALOG_TITLE)
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
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.Clickables.END_TIME)
            )
        }
    }

    @Composable
    fun MaxParticipantsRow(
        maxParticipants: Int,
        onMaxParticipantsChange: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Max Participants: ",
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.Texts.MAX_PARTICIPANTS_TEXT)
            )
            val focusManager = LocalFocusManager.current
            TextField(
                value = if (maxParticipants != 0) {
                    maxParticipants.toString()
                } else {
                    ""
                },
                onValueChange = {
                    onMaxParticipantsChange(it.toIntOrNull() ?: 0)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.TextFields.MAX_PARTICIPANTS)
            )
        }
    }

    @Composable
    fun EventSportRow(
        sport: List<Sports>,
        onSportChange: (List<Sports>) -> Unit,
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Sport: ",
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.Texts.SPORT_TEXT)
            )

            Box(
                modifier = Modifier
                    .clickable {
                        selectSportsHandler(
                            SelectSportsActivity.getIntent(
                                context = context,
                                initialValue = sport
                            )
                        ).thenApply {
                            // only update the sport if the user selected only one
                            if (it.size == 1) {
                                onSportChange(it)
                            }
                        }
                    }
                    .weight(.8f)
                    .testTag(TestTags.Clickables.SPORT)
            ) {
                sport.forEach {
                    Spacer(modifier = Modifier.padding(0.dp, 0.dp, 6.dp, 0.dp))
                    Icon(
                        imageVector = it.sportIcon,
                        contentDescription = it.sportName,
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 6.dp, 0.dp)
                            .size(16.dp)
                            .testTag(TestTags.SportElement(it).ICON)
                    )
                }
            }
        }
    }

    @Composable
    fun EventLocationRow(
        location: Address,
        onLocationChange: (Address) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Location: ",
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.Texts.LOCATION_TEXT)
            )
            ClickableText(
                text = AnnotatedString(location.name),
                onClick = {
                    addressAutocompleteHandler.launch(
                    ).thenApply {
                        onLocationChange(it)
                    }
                },
                style = MaterialTheme.typography.body1.copy(
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                ),
                modifier = Modifier
                    .weight(.8f)
                    .testTag(TestTags.Clickables.LOCATION)
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
                    .testTag(TestTags.Texts.COLOR_TEXT)
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
                            .testTag(TestTags.Texts.COLOR_DIALOG_TITLE)
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
                    .testTag(TestTags.Clickables.COLOR_BOX)
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
                    .testTag(TestTags.TextFields.DESCRIPTION)
            )
        }
    }
}




