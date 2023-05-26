@file:OptIn(ExperimentalComposeUiApi::class)

package com.github.sdpcoachme.schedule

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.github.sdpcoachme.profile.AttributeRow
import com.github.sdpcoachme.profile.EditTextActivity
import com.github.sdpcoachme.profile.SelectSportsActivity
import com.github.sdpcoachme.profile.SportsRow
import com.github.sdpcoachme.profile.TextRow
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
                val EVENT_TITLE_TEXT = text("eventName")
                val START_DATE_TEXT = text("startDate")
                val START_TIME_TEXT = text("startTime")
                val END_DATE_TEXT = text("endDate")
                val END_TIME_TEXT = text("endTime")
                val MAX_PARTICIPANTS_TEXT = text("maxParticipants")
                val LOCATION_TEXT = text("location")
                val DESCRIPTION_TEXT = text("description")

                val START_DATE_DIALOG_TITLE = text("startDateDialogTitle")
                val END_DATE_DIALOG_TITLE = text("endDateDialogTitle")
                val START_TIME_DIALOG_TITLE = text("startTimeDialogTitle")
                val END_TIME_DIALOG_TITLE = text("endTimeDialogTitle")
                val COLOR_DIALOG_TITLE = text("colorDialogTitle")
            }
        }

        class Clickables {
            companion object {
                private fun button(tag: String): String {
                    return "${tag}Button"
                }

                val SAVE = button("save")
                val CANCEL = button("cancel")
                val COLOR_BOX = "colorBox"
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

        class Label {
            companion object {
                val EVENT_TITLE_LABEL = "EVENT TITLE"
                val START_DATE_LABEL = "START DATE"
                val START_TIME_LABEL = "START TIME"
                val END_DATE_LABEL = "END DATE"
                val END_TIME_LABEL = "END TIME"
                val MAX_PARTICIPANTS_LABEL = "MAX PARTICIPANTS"
                val SPORT_LABEL = "SPORT"
                val LOCATION_LABEL = "LOCATION"
                val COLOR_LABEL = "COLOR"
                val DESCRIPTION_LABEL = "DESCRIPTION"
            }
        }

        companion object {
            const val SCAFFOLD = "scaffold"
            const val SPORTS = "sports"
        }
    }

    private lateinit var store: CachingStore
    private lateinit var addressAutocompleteHandler: AddressAutocompleteHandler
    private lateinit var selectSportsHandler: (Intent) -> CompletableFuture<List<Sports>>
    private lateinit var editTextHandler: (Intent) -> CompletableFuture<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store

        // Set up handler for calls to location autocomplete
        addressAutocompleteHandler = (application as CoachMeApplication).addressAutocompleteHandler(this, this)

        // Set up handler for calls to select sports activity
        selectSportsHandler = SelectSportsActivity.getHandler(this)

        // Set up handler for calls to edit text activity
        editTextHandler = EditTextActivity.getHandler(this)

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
                    address = location,
                    description = description
                )
                TopAppBar(
                    title = {
                        Text(
                            text = "New event",
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
                                if (start.isAfter(end) or start.isEqual(end)) {
                                    val toast = Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_SHORT)
                                    toast.show()
                                } else {
                                    if (eventType == EventType.PRIVATE) {
                                        EventOps.addEvent(event, store).thenAccept {
                                            finish() // Go back to previous activity (removes itself from activity stack)
                                        }
                                    } else if (eventType == EventType.GROUP) {
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
                // TODO: uncomment this to enable dark mode for maxkeppeler sheets
                //CoachMeMaterial3Theme {
                    val scrollState = rememberScrollState()
                    Column (
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .verticalScroll(scrollState)
                    ) {
                        TextRow(
                            label = TestTags.Label.EVENT_TITLE_LABEL,
                            tag = TestTags.Texts.EVENT_TITLE_TEXT,
                            value = eventName,
                            onClick = {
                                editTextHandler(
                                    EditTextActivity.getIntent(
                                        context = context,
                                        initialValue = eventName,
                                        label = "Event title",
                                    )
                                ).thenApply {
                                    eventName = it
                                }
                            }
                        )

                        Divider(startIndent = 20.dp)
                        // Start date
                        StartDateRow(
                            startDateSheet = startDateSheet,
                            start = start,
                            formatter = formatterUserDate,
                            onDateChange = { start = it },
                        )
                        Divider(startIndent = 20.dp)
                        StartTimeRow(
                            startTimeSheet = startTimeSheet,
                            start = start,
                            formatter = formatterUserTime,
                            onTimeChange = { start = it }
                        )
                        Divider(startIndent = 20.dp)
                        // End date
                        EndDateRow(
                            endDateSheet = endDateSheet,
                            end = end,
                            formatter = formatterUserDate,
                            onDateChange = { end = it },
                        )
                        Divider(startIndent = 20.dp)
                        EndTimeRow(
                            endTimeSheet = endTimeSheet,
                            end = end,
                            formatter = formatterUserTime,
                            onTimeChange = { end = it }
                        )

                        if (eventType == EventType.GROUP) {
                            Divider(startIndent = 20.dp)
                            MaxParticipantsRow(
                                maxParticipants = maxParticipants,
                                onMaxParticipantsChange = { maxParticipants = it }
                            )
                        }

                        Divider(startIndent = 20.dp)
                        SportsRow(
                            label = TestTags.Label.SPORT_LABEL,
                            tag = TestTags.SPORTS,
                            value = sports,
                            onClick = {
                                selectSportsHandler(
                                    SelectSportsActivity.getIntent(
                                        context = context,
                                        initialValue = sports,
                                    )
                                ).thenApply {
                                    // only update the sport if the user selected only one
                                    if (it.size == 1) {
                                        sports = it
                                    }
                                }
                            }
                        )
                        Divider(startIndent = 20.dp)
                        EventLocationRow(
                            location = location,
                            onLocationChange = { location = it }
                        )
                        Divider(startIndent = 20.dp)
                        // Color
                        ColorRow(
                            colorSheet = colorSheet,
                            selectedColor = selectedColor,
                            onColorChange = { selectedColor = it }
                        )
                        Divider(startIndent = 20.dp)
                        // Description
                        DescriptionRow(
                            description = description,
                            onDescriptionChange = { description = it }
                        )
                    }
                //}
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
        AttributeRow(
            label = TestTags.Label.START_DATE_LABEL,
            onClick = { startDateSheet.show() },
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.START_DATE_TEXT),
                text = start.format(formatter),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CalendarDialog(
                state = startDateSheet,
                header = Header.Custom {
                    Text(
                        text = "Start date",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.START_DATE_DIALOG_TITLE),
                        // TODO: temporary until dark mode is enabled for maxkeppeler sheets
                        color = Color.Black
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
        }
    }

    @Composable
    fun StartTimeRow(
        startTimeSheet: SheetState,
        start: LocalDateTime,
        formatter: DateTimeFormatter,
        onTimeChange: (LocalDateTime) -> Unit
    ) {
        AttributeRow(
            label = TestTags.Label.START_TIME_LABEL,
            onClick = { startTimeSheet.show() },
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.START_TIME_TEXT),
                text = start.format(formatter),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ClockDialog(
                state = startTimeSheet,
                header = Header.Custom {
                    Text(
                        text = "Start time",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.START_TIME_DIALOG_TITLE),
                        // TODO: temporary until dark mode is enabled for maxkeppeler sheets
                        color = Color.Black
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
        }
    }

    @Composable
    fun EndDateRow(
        endDateSheet: SheetState,
        end: LocalDateTime,
        formatter: DateTimeFormatter,
        onDateChange: (LocalDateTime) -> Unit
    ) {
        AttributeRow(
            label = TestTags.Label.END_DATE_LABEL,
            onClick = { endDateSheet.show() },
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.END_DATE_TEXT),
                text = end.format(formatter),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CalendarDialog(
                state = endDateSheet,
                header = Header.Custom {
                    Text(
                        text = "End date",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.END_DATE_DIALOG_TITLE),
                        // TODO: temporary until dark mode is enabled for maxkeppeler sheets
                        color = Color.Black
                    )
                },
                config = CalendarConfig(
                    monthSelection = false,
                    yearSelection = false,
                    style = CalendarStyle.MONTH),
                selection = CalendarSelection.Date {
                    onDateChange(it.atStartOfDay().plusHours(1))
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = true,
                )
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
        AttributeRow(
            label = TestTags.Label.END_TIME_LABEL,
            onClick = { endTimeSheet.show() },
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.END_TIME_TEXT),
                text = end.format(formatter),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ClockDialog(
                state = endTimeSheet,
                header = Header.Custom {
                    Text(
                        text = "End time",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.END_TIME_DIALOG_TITLE),
                        // TODO: temporary until dark mode is enabled for maxkeppeler sheets
                        color = Color.Black
                    )
                },
                config = ClockConfig(
                    defaultTime = end.toLocalTime(),
                    is24HourFormat = true
                ),
                selection = ClockSelection.HoursMinutes { hours, minutes ->
                    onTimeChange(end.withHour(hours).withMinute(minutes))
                }
            )
        }
    }

    @Composable
    fun MaxParticipantsRow(
        maxParticipants: Int,
        onMaxParticipantsChange: (Int) -> Unit
    ) {
        val context = LocalContext.current
        AttributeRow(
            label = TestTags.Label.MAX_PARTICIPANTS_LABEL,
            onClick = {
                editTextHandler(
                    EditTextActivity.getIntent(
                        context = context,
                        initialValue =
                            if (maxParticipants != 0) {
                                maxParticipants.toString()
                            } else {
                                ""
                            },
                        label = "Max participants",
                    )
                ).thenApply {
                    onMaxParticipantsChange(it.toIntOrNull() ?: 0)
                }
            }
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.MAX_PARTICIPANTS_TEXT),
                text = maxParticipants.toString(),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun EventLocationRow(
        location: Address,
        onLocationChange: (Address) -> Unit,
    ) {
        AttributeRow(
            label = TestTags.Label.LOCATION_LABEL,
            onClick = {
                addressAutocompleteHandler.launch(
                ).thenApply {
                    onLocationChange(it)
                }
            }
        ) {
            Text(
                modifier = Modifier.testTag(TestTags.Texts.LOCATION_TEXT),
                text = location.name,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ColorRow(
        colorSheet: SheetState,
        selectedColor: Color,
        onColorChange: (Color) -> Unit
    ) {
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
        AttributeRow(
            label = TestTags.Label.COLOR_LABEL,
            onClick = { colorSheet.show() },
        ) {
            Box (
                modifier = Modifier
                    .fillMaxHeight(.5f)
                    .aspectRatio(1f, true)
                    .size(5.dp)
                    .clickable { colorSheet.show() }
                    .background(selectedColor)
                    .testTag(TestTags.Clickables.COLOR_BOX)
            )
            ColorDialog (
                state = colorSheet,
                header = Header.Custom {
                    Text(
                        text = "Select color",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Texts.COLOR_DIALOG_TITLE),
                        // TODO: temporary until dark mode is enabled for maxkeppeler sheets
                        color = Color.Black
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
        }
    }

    @Composable
    fun DescriptionRow(
        description: String,
        onDescriptionChange: (String) -> Unit
    ) {
        val context = LocalContext.current
        AttributeRow(
            label = TestTags.Label.DESCRIPTION_LABEL,
            modifier = Modifier
                .fillMaxHeight(),
            onClick = {
                editTextHandler(
                    EditTextActivity.getIntent(
                        context = context,
                        initialValue = description,
                        label = "Description",
                    )
                ).thenApply {
                    onDescriptionChange(it)
                }
            },
        ) {
            Text(
                modifier = Modifier
                    .testTag(TestTags.Texts.DESCRIPTION_TEXT)
                    .fillMaxHeight(),
                text = description,
                style = MaterialTheme.typography.body1,
                maxLines = 20,
                softWrap = true,
                overflow = TextOverflow.Visible
            )
        }
    }
}




