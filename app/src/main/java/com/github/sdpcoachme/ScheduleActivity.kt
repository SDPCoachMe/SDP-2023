package com.github.sdpcoachme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

class ScheduleActivity : ComponentActivity() {
    private lateinit var database: Database
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("email")

        if (email == null) {
            val errorMsg = "Profile editing did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            database = (application as CoachMeApplication).database

            //TODO: For demo, let this function run once to add sample events to the database
            database.addEventsToDatabase(email, sampleEvents).thenRun {
                val futureUserInfo: CompletableFuture<UserInfo> = database.getUser(email)

                setContent {
                    CoachMeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            Schedule(futureUserInfo)
                        }
                    }
                }
            }
        }
    }

    class TestTags {
        class ScheduleHeader(tag: String) {
            val ROW = "${tag}Row"
        }
        class BasicSchedule(tag: String) {
            val Layout = "${tag}Layout"
            val COLUMN = "${tag}Column"
        }
        companion object {
            const val SCHEDULE_COLUMN = "scheduleColumn"
            const val SCHEDULE_HEADER = "scheduleHeader"
            const val BASIC_SCHEDULE = "basicSchedule"
            const val EVENT = "event"
            const val EVENT_NAME = "eventName"
            const val EVENT_TIME = "eventTime"
            const val EVENT_DESCRIPTION = "eventDescription"

            val SCHEDULE_HEADER_ROW = ScheduleHeader(SCHEDULE_HEADER).ROW
            val BASIC_SCHEDULE_LAYOUT = BasicSchedule(BASIC_SCHEDULE).Layout
        }
    }
}

private class EventDataModifier(val event: Event) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = event
}

@Composable
fun Schedule(
    futureUserInfo: CompletableFuture<UserInfo>,
    modifier: Modifier = Modifier,
) {
    // bind those to database
    var events by remember { mutableStateOf(emptyList<Event>()) }
    var eventsFuture by remember { mutableStateOf(futureUserInfo.thenApply { it.events }) }

    LaunchedEffect(eventsFuture) {
        eventsFuture.thenAccept { e ->
            if (e != null) {
                events = e
                val f = CompletableFuture<List<Event>?>()
                f.complete(null)
                eventsFuture = f
            }
        }.exceptionally { null }
    }

    // the starting day is always the previous Monday
    val minDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val maxDate = minDate.plusDays(ColumnsPerWeek.toLong())
    val dayWidth = LocalConfiguration.current.screenWidthDp.dp / ColumnsPerWeek
    val hourHeight = 64.dp
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    Column(modifier = modifier.testTag(ScheduleActivity.TestTags.SCHEDULE_COLUMN)) {
        ScheduleHeader(
            minDate = minDate,
            maxDate = maxDate,
            dayWidth = dayWidth,
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .testTag(ScheduleActivity.TestTags.SCHEDULE_HEADER)
        )
        BasicSchedule(
            events = events,
            minDate = minDate,
            dayWidth = dayWidth,
            hourHeight = hourHeight,
            modifier = Modifier
                .weight(1f) // Fill remaining space in the column
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .testTag(ScheduleActivity.TestTags.BASIC_SCHEDULE)
        )
    }
}

private fun Modifier.eventData(event: Event) = this.then(EventDataModifier(event))
private val EventTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val WithoutNanoSecondsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val DayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private const val ColumnsPerWeek = 7

@Composable
fun BasicEvent(
    event: Event,
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
            text = "${event.start.format(EventTimeFormatter)} - ${event.end.format(EventTimeFormatter)}",
            style = MaterialTheme.typography.caption,
            fontSize = 9f.sp,
        )

        Text(
            text = event.name,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            fontSize = 12f.sp,
        )

        //TODO: Only show description when event expanded
        Text(
            text = event.description,
            style = MaterialTheme.typography.body2,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10f.sp,
        )
    }
}

@Composable
fun BasicDayHeader(
    day: LocalDate,
    modifier: Modifier = Modifier,
) {
    Text(
        text = day.format(DayFormatter),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        fontSize = 12f.sp,
    )
}

@Composable
fun ScheduleHeader(
    minDate: LocalDate,
    maxDate: LocalDate,
    dayWidth: Dp,
    modifier: Modifier = Modifier,
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) }
) {
    Row(modifier = modifier) {
        val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
        repeat(numDays) { i ->
            Box(modifier = Modifier.width(dayWidth)) {
                dayHeader(minDate.plusDays(i.toLong()))
            }
        }
    }
}

@Composable
fun BasicSchedule(
    events: List<Event>,
    modifier: Modifier = Modifier,
    minDate: LocalDate = LocalDateTime.parse(events.minByOrNull(Event::start)!!.start).toLocalDate(),
    dayWidth: Dp,
    hourHeight: Dp,
) {
    val dividerColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray
    Layout(
        content = {
            events.sortedBy(Event::start).forEach { event ->
                // Pass the event as parent data to the eventContent composable
                Box(modifier = Modifier.eventData(event)) {
                    BasicEvent(event = event)
                }
            }
        },
        modifier = modifier.testTag(ScheduleActivity.TestTags.BASIC_SCHEDULE_LAYOUT)
            .drawBehind {   //add dividers (lines) between days and hours
                repeat(23) {
                    drawLine(
                        dividerColor,
                        start = Offset(0f, (it + 1) * hourHeight.toPx()),
                        end = Offset(size.width, (it + 1) * hourHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                repeat(ColumnsPerWeek - 1) {
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
        val width = dayWidth.roundToPx() * ColumnsPerWeek
        val placeablesWithEvents = measureables.map { measurable ->
            val event = measurable.parentData as Event
            val eventDurationMinutes = ChronoUnit.MINUTES.between(LocalDateTime.parse(event.start), LocalDateTime.parse(event.end))
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val placeable = measurable.measure(constraints.copy(minWidth = dayWidth.roundToPx(), maxWidth = dayWidth.roundToPx(), minHeight = eventHeight, maxHeight = eventHeight))
            Pair(placeable, event)
        }
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, event) ->
                val eventOffsetMinutes = ChronoUnit.MINUTES.between(LocalTime.MIN, LocalDateTime.parse(event.start).toLocalTime())
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                val eventOffsetDays = ChronoUnit.DAYS.between(minDate, LocalDateTime.parse(event.start).toLocalDate()).toInt()
                val eventX = eventOffsetDays * dayWidth.roundToPx()
                placeable.place(eventX, eventY)
            }
        }
    }
}

private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

// mainly for testing, debugging and demo purposes
private val sampleEvents = listOf(
    Event(
        name = "Google I/O Keynote",
        color = Color(0xFFAFBBF2).value.toString(),
        start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0).toString(),
        end = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0).toString(),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
    ),
    Event(
        name = "Developer Keynote",
        color = Color(0xFFAFBBF2).value.toString(),
        start = currentMonday.plusDays(2).atTime(7, 0, 0).toString(),
        end = currentMonday.plusDays(2).atTime(9, 0, 0).toString(),
        description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
    ),
    Event(
        name = "What's new in Android",
        color = Color(0xFF1B998B).value.toString(),
        start = currentMonday.plusDays(2).atTime(10, 0, 0).toString(),
        end = currentMonday.plusDays(2).atTime(12, 0, 0).toString(),
        description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
    ),
    Event(
        name = "What's new in Machine Learning",
        color = Color(0xFFF4BFDB).value.toString(),
        start = currentMonday.plusDays(2).atTime(22, 0, 0).toString(),
        end = currentMonday.plusDays(3).atTime(4, 0, 0).toString(),
        description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
    ),
    Event(
        name = "What's new in Material Design",
        color = Color(0xFF6DD3CE).value.toString(),
        start = currentMonday.plusDays(3).atTime(13, 0, 0).toString(),
        end = currentMonday.plusDays(3).atTime(15, 0, 0).toString(),
        description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
    ),
    Event(
        name = "Jetpack Compose Basics",
        color = Color(0xFF1B998B).value.toString(),
        start = currentMonday.plusDays(4).atTime(9, 0, 0).toString(),
        end = currentMonday.plusDays(4).atTime(13, 0, 0).toString(),
        description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
    ),
)
