package com.github.sdpcoachme.schedule


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventType
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.location.provider.FusedLocationProvider
import com.github.sdpcoachme.schedule.EventOps.Companion.getDayFormatter
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import com.github.sdpcoachme.schedule.EventOps.Companion.getTimeFormatter
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.Purple500
import com.github.sdpcoachme.weather.WeatherForecast
import com.github.sdpcoachme.weather.WeatherView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

class ScheduleActivity : ComponentActivity() {
    class TestTags {
        class BasicSchedule(tag: String) {
            val layout = "${tag}Layout"
        }

        class Buttons {
            companion object {
                const val LEFT_ARROW_BUTTON = "leftArrowButton"
                const val RIGHT_ARROW_BUTTON = "rightArrowButton"
                const val BACK = "backButton"
                const val ADD_EVENT_BUTTON = "addEventButton"
                const val ADD_PRIVATE_EVENT_BUTTON = "addPrivateEventButton"
                const val ADD_GROUP_EVENT_BUTTON = "addGroupEventButton"
            }
        }
        class TextFields {
            companion object {
                const val CURRENT_WEEK_TEXT_FIELD = "currentWeekTextField"
            }
        }
        companion object {
            const val SCHEDULE_COLUMN = "scheduleColumn"
            const val BASIC_SCHEDULE = "basicSchedule"
            const val WEEK_HEADER = "weekHeader"

            val BASIC_SCHEDULE_LAYOUT = BasicSchedule(BASIC_SCHEDULE).layout

        }
    }

    private lateinit var store: CachingStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = (application as CoachMeApplication).store

        val startMonday = getStartMonday()

        val futureDBSchedule: CompletableFuture<Schedule> = store.getSchedule(startMonday)

        // todo move this into caching store
        val locationProvider = (application as CoachMeApplication).locationProvider
        locationProvider.updateContext(this, CompletableFuture.completedFuture(null))
        val userLatLng = locationProvider.getLastLocation().value?: FusedLocationProvider.CAMPUS
        val weatherState = store.getWeatherForecast(userLatLng).get()

        setContent {
            CoachMeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(futureDBSchedule, store, weatherState)
                }
            }
        }
    }
}

private class EventDataModifier(val event: ShownEvent) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = event
}

private const val COLUMNS_PER_WEEK = 7
@Composable
fun Schedule(
    futureDBSchedule: CompletableFuture<Schedule>,
    store: CachingStore,
    weatherState: MutableState<WeatherForecast>,
    modifier: Modifier = Modifier
    ) {
    // the starting day is always the monday of the current week
    var shownWeekMonday by remember { mutableStateOf(getStartMonday()) }
    var events by remember { mutableStateOf(emptyList<Event>()) }
    var eventsFuture by remember { mutableStateOf(futureDBSchedule.thenApply {
        Schedule(events = it.events) })
    }
    val context = LocalContext.current

    // Launch an effect when the eventsFuture changes
    LaunchedEffect(eventsFuture) {
        eventsFuture.thenAccept { e ->
            if (e != null) {
                events = e.events
                eventsFuture = CompletableFuture.completedFuture(null)
            }
        }.exceptionally {
            val errorMsg = "Schedule couldn't get the user information from the database." +
                "\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(context, errorMsg)
            null
        }
    }

    val dayWidth = LocalConfiguration.current.screenWidthDp.dp / COLUMNS_PER_WEEK
    val verticalScrollState = rememberScrollState()

    fun updateCurrentWeekMonday(weeksToAdd: Int) {
        shownWeekMonday = shownWeekMonday.plusWeeks(weeksToAdd.toLong())
        // Update the cached events and if not already cached, get the events from the database
        store.getSchedule(shownWeekMonday).thenAccept { schedule ->
            events = schedule.events
        }
    }

    Box(modifier = modifier) {
        Column(modifier = modifier
            .testTag(ScheduleActivity.TestTags.SCHEDULE_COLUMN)
        ) {
            ScheduleTitleRow(
                shownWeekMonday = shownWeekMonday,
                onLeftArrowClick = { updateCurrentWeekMonday(-1) },
                onRightArrowClick = { updateCurrentWeekMonday(1) },
            )

            WeekHeader(
                shownWeekMonday = shownWeekMonday,
                dayWidth = dayWidth,
                weatherState = weatherState
            )

            // filter events to only show events in the current week
            val eventsToShow = EventOps.eventsToWrappedEvents(events)

            BasicSchedule(
                events = eventsToShow.filter { event ->
                    val eventDate = LocalDateTime.parse(event.start).toLocalDate()
                    eventDate >= shownWeekMonday && eventDate < shownWeekMonday.plusWeeks(1)
                },
                shownMonday = shownWeekMonday,
                dayWidth = dayWidth,
                modifier = Modifier
                    .weight(1f) // Fill remaining space in the column
                    .verticalScroll(verticalScrollState)
                    .testTag(ScheduleActivity.TestTags.BASIC_SCHEDULE)
            )
        }

        fun launchCreateEventActivity(eventType: EventType) {
            val intent = Intent(context, CreateEventActivity::class.java)
            intent.putExtra("eventType", eventType.eventTypeName)
            context.startActivity(intent)
        }

        var isDropdownExpanded by remember { mutableStateOf(false) }

        FloatingActionButton(
            onClick = {
                val isCoachFuture = store.getCurrentEmail().thenCompose { email ->
                    store.getUser(email)
                }.thenApply { user ->
                    user.coach
                }
                // if user is coach, let them choose between private and group event
                isCoachFuture.thenAccept { isCoach ->
                    if (isCoach) {
                        isDropdownExpanded = !isDropdownExpanded
                    } else {
                        launchCreateEventActivity(EventType.PRIVATE)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag(ScheduleActivity.TestTags.Buttons.ADD_EVENT_BUTTON),
            backgroundColor = Purple500
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Event"
            )
        }

        // TODO: align the dropdown menu with the add event button
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
            properties = PopupProperties(clippingEnabled = false),
        ) {
            DropdownMenuItem(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ScheduleActivity.TestTags.Buttons.ADD_PRIVATE_EVENT_BUTTON),
                onClick = {
                    isDropdownExpanded = false
                    launchCreateEventActivity(EventType.PRIVATE)
                }
            ) {
                Text(text = "Private Event")
            }
            DropdownMenuItem(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ScheduleActivity.TestTags.Buttons.ADD_GROUP_EVENT_BUTTON),
                onClick = {
                    isDropdownExpanded = false
                    launchCreateEventActivity(EventType.GROUP)
                }
            ) {
                Text(text = "Group Event")
            }
        }
    }
}

@Composable
fun ScheduleTitleRow(
    shownWeekMonday: LocalDate,
    onLeftArrowClick: () -> Unit,
    onRightArrowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = { onLeftArrowClick() },
                modifier = Modifier
                    .testTag(ScheduleActivity.TestTags.Buttons.LEFT_ARROW_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = "Left arrow",
                    tint = Color.White
                )
            }

            val formatter = DateTimeFormatter.ofPattern("d MMM")
            // To prevent the text from being too long, set a max width
            val maxTextWidth = LocalConfiguration.current.screenWidthDp.dp / 5
            Text(
                text = "${shownWeekMonday.format(formatter)} - \n${
                    shownWeekMonday.plusDays(6).format(formatter)
                }",
                style = MaterialTheme.typography.subtitle1,
                color = Color.White,
                modifier = Modifier
                    .testTag(ScheduleActivity.TestTags.TextFields.CURRENT_WEEK_TEXT_FIELD)
                    .align(Alignment.CenterVertically)
                    .widthIn(max = maxTextWidth)
            )


            IconButton(
                onClick = { onRightArrowClick() },
                modifier = Modifier
                    .testTag(ScheduleActivity.TestTags.Buttons.RIGHT_ARROW_BUTTON)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowRight,
                    contentDescription = "Right arrow",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun WeekHeader(
    shownWeekMonday: LocalDate,
    dayWidth: Dp,
    modifier: Modifier = Modifier,
    weatherState: MutableState<WeatherForecast>,
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) }
) {
    Row(
        modifier = modifier
            .testTag(ScheduleActivity.TestTags.WEEK_HEADER)
    ) {
        repeat(COLUMNS_PER_WEEK) { i ->
            val day = shownWeekMonday.plusDays(i.toLong())
            Column(modifier = Modifier.width(dayWidth)) {
                dayHeader(day)
                WeatherView(weatherState, day)
            }
        }
    }
}

@Composable
fun BasicDayHeader(
    day: LocalDate,
    modifier: Modifier = Modifier,
) {
    val textWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
    Text(
        text = day.format(DayFormatter),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        fontSize = 12f.sp,
        fontWeight = textWeight,
    )
}

@Composable
fun BasicSchedule(
    events: List<ShownEvent>,
    modifier: Modifier = Modifier,
    shownMonday: LocalDate,
    dayWidth: Dp,
    hourHeight: Dp = 64.dp,
) {
    val dividerColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray
    Layout(
        content = {
            events.sortedBy(ShownEvent::start).forEach { event ->
                // Pass the event as parent data to the eventContent composable
                Box(modifier = Modifier.eventData(event)) {
                    BasicEvent(event = event)
                }
            }
        },
        modifier = modifier
            .testTag(ScheduleActivity.TestTags.BASIC_SCHEDULE_LAYOUT)
            .drawBehind {   //add dividers (lines) between days and hours
                repeat(23) {
                    drawLine(
                        dividerColor,
                        start = Offset(0f, (it + 1) * hourHeight.toPx()),
                        end = Offset(size.width, (it + 1) * hourHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                repeat(COLUMNS_PER_WEEK - 1) {
                    drawLine(
                        dividerColor,
                        start = Offset((it + 1) * dayWidth.toPx(), 0f),
                        end = Offset((it + 1) * dayWidth.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            },
    ) { measureables, constraints ->
        val height = hourHeight.roundToPx() * 24
        val width = dayWidth.roundToPx() * COLUMNS_PER_WEEK
        // This part measures the events and ensures that the event size corresponds to the event duration
        val placeablesWithEvents = measureables.map { measurable ->
            val event = measurable.parentData as ShownEvent
            val eventDurationMinutes = ChronoUnit.MINUTES.between(LocalDateTime.parse(event.start), LocalDateTime.parse(event.end))
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val placeable = measurable.measure(constraints.copy(minWidth = dayWidth.roundToPx(), maxWidth = dayWidth.roundToPx(), minHeight = eventHeight, maxHeight = eventHeight))
            Pair(placeable, event)
        }
        // This part ensures that the events are placed correctly
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, event) ->
                val eventOffsetMinutes = ChronoUnit.MINUTES.between(LocalTime.MIN, LocalDateTime.parse(event.start).toLocalTime())
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                val eventOffsetDays = ChronoUnit.DAYS.between(shownMonday, LocalDateTime.parse(event.start).toLocalDate()).toInt()
                val eventX = eventOffsetDays * dayWidth.roundToPx()
                placeable.place(eventX, eventY)
            }
        }
    }
}

private fun Modifier.eventData(event: ShownEvent) = this.then(EventDataModifier(event))
private val EventTimeFormatter: DateTimeFormatter = getTimeFormatter()
private val DayFormatter = getDayFormatter()

@Composable
fun BasicEvent(
    event: ShownEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 2.dp, bottom = 2.dp)
            .background(Color(event.color.toULong()), shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Text(
            text = "${LocalDateTime.parse(event.startText).toLocalTime().format(EventTimeFormatter)} - ${LocalDateTime.parse(event.endText).toLocalTime().format(EventTimeFormatter)}",
            style = MaterialTheme.typography.caption,
            fontSize = 9f.sp,
        )

        Text(
            text = event.name,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            fontSize = 12f.sp,
        )

        Text(
            text = event.description,
            style = MaterialTheme.typography.body2,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10f.sp,
        )
    }
}
