package com.github.sdpcoachme.schedule


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.schedule.EventOps.Companion.getDayFormatter
import com.github.sdpcoachme.schedule.EventOps.Companion.getEventTimeFormatter
import com.github.sdpcoachme.schedule.EventOps.Companion.getStartMonday
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.github.sdpcoachme.ui.theme.Purple500
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
    class TestTags {
        class BasicSchedule(tag: String) {
            val layout = "${tag}Layout"
        }

        class Buttons {
            companion object {
                const val LEFT_ARROW_BUTTON = "leftArrowButton"
                const val RIGHT_ARROW_BUTTON = "rightArrowButton"
                const val BACK = "backButton"
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

    private lateinit var database: Database
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()

        if (email.isEmpty()) {
            val errorMsg = "Schedule did not receive an email address.\n Please return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val startMonday = getStartMonday()
            //TODO: For demo, let this function run once to add sample events to the database
            //database.addEvents(sampleEvents, startMonday).thenRun {
                val futureDBSchedule: CompletableFuture<Schedule> = database.getSchedule(startMonday).exceptionally {
                    println("ScheduleActivity: Error getting schedule from database")
                    null
                }

                setContent {
                    CoachMeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            Schedule(futureDBSchedule, database)
                        }
                    }
                }
            //}
        }
    }
}

private class EventDataModifier(val event: ShownEvent) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = event
}

private const val ColumnsPerWeek = 7
@Composable
fun Schedule(
    futureDBSchedule: CompletableFuture<Schedule>,
    database: Database,
    modifier: Modifier = Modifier,
) {
    // the starting day is always the monday of the current week
    var shownWeekMonday by remember { mutableStateOf<LocalDate>(getStartMonday()) }
    var events by remember { mutableStateOf(emptyList<Event>()) }
    var eventsFuture by remember { mutableStateOf(futureDBSchedule.thenApply { Schedule(events = it.events) }) }
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

    val dayWidth = LocalConfiguration.current.screenWidthDp.dp / ColumnsPerWeek
    val verticalScrollState = rememberScrollState()


    fun updateCurrentWeekMonday(weeksToAdd: Int) {
        shownWeekMonday = shownWeekMonday.plusWeeks(weeksToAdd.toLong())
        // Update the cached events and if not already cached, get the events from the database
        database.getSchedule(shownWeekMonday).thenAccept { schedule ->
            events = schedule.events
        }
    }

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
        )

        // filter events to only show events in the current week
        val eventsToShow = EventOps.eventsToWrappedEvents(events)
        BasicSchedule(
            events = eventsToShow.filter {event ->
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
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) }
) {
    Row(
        modifier = modifier
            .testTag(ScheduleActivity.TestTags.WEEK_HEADER)
    ) {
        repeat(ColumnsPerWeek) {i ->
            val day = shownWeekMonday.plusDays(i.toLong())
            Box(modifier = Modifier.width(dayWidth)) {
                dayHeader(day)
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
private val EventTimeFormatter: DateTimeFormatter = getEventTimeFormatter()
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


// --------------------------------------------------
// mainly for testing, debugging and demo purposes
private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
private val lastMonday = currentMonday.minusDays(7)
private val nextMonday = currentMonday.plusDays(7)
private val sampleEvents = listOf(
    Event(
        name = "Google I/O Keynote",
        color = Color(0xFFAFBBF2).value.toString(),
        start = lastMonday.plusDays(1).atTime(13, 0, 0).toString(),
        end = lastMonday.plusDays(1).atTime(15, 0, 0).toString(),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
    ),
    Event(
        name = "Business Trip",
        color = Color(0xFFC9A776).value.toString(),
        start = lastMonday.plusDays(4).atTime(9, 0, 0).toString(),
        end = currentMonday.plusDays(1).atTime(18, 0, 0).toString(),
        description = "I'm going to be out of the office for a business trip.",
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
        color = Color(0xFF549C94).value.toString(),
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
        color = Color(0xFFC08A78).value.toString(),
        start = currentMonday.plusDays(3).atTime(13, 0, 0).toString(),
        end = currentMonday.plusDays(3).atTime(15, 0, 0).toString(),
        description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
    ),
    Event(
        name = "Jetpack Compose Basics",
        color = Color(0xFFB98FC0).value.toString(),
        start = nextMonday.plusDays(4).atTime(9, 0, 0).toString(),
        end = nextMonday.plusDays(4).atTime(13, 0, 0).toString(),
        description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
    ),
    Event(
        name = "Holidays",
        color = Color(0xFF71A5CE).value.toString(),
        start = currentMonday.plusDays(4).atTime(14, 0, 0).toString(),
        end = nextMonday.plusDays(1).atTime(18, 0, 0).toString(),
        description = "A few days off to relax and enjoy the holidays.",
    ),
)
