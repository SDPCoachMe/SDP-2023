package com.github.sdpcoachme.rating

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.Buttons.Companion.DONE
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_BAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.RATING_STAR
import com.github.sdpcoachme.rating.RatingActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.ui.theme.selectedStar
import com.github.sdpcoachme.ui.theme.unselectedStar


@Composable
fun RatingView(
    title: String,
    initialRating: Int,
    onCancel: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var rating by remember { mutableStateOf(initialRating) }
    var selected by remember { mutableStateOf(false) }
    val size by animateDpAsState(
        targetValue = if (selected) 72.dp else 64.dp,
        animationSpec = spring(DampingRatioMediumBouncy)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, modifier = Modifier.testTag(TITLE))
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag(CANCEL)) {
                        Icon(
                            imageVector = Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onSubmit(rating) }, modifier = Modifier.testTag(DONE)) {
                        Icon(
                            imageVector = Filled.Done,
                            contentDescription = "Done",
                            tint = if (colors.isLight) colors.onPrimary else colors.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
            .clickable { rating = 0 }
            .padding(padding)
            .fillMaxSize(),
            verticalArrangement = Center,
            horizontalAlignment = CenterHorizontally
        ) {
            RatingBar(
                modifier = Modifier.testTag(RATING_BAR),
                padding = padding,
                size = size,
                rating = rating,
                onTouchEvent = { star, it ->
                    when (it.action) {
                        ACTION_DOWN -> {
                            selected = true
                            rating = star
                        }
                        ACTION_UP -> selected = false
                    }
                    true
                }
            )
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    size: Dp = 20.dp,
    rating: Int,
    onTouchEvent: (Int, MotionEvent) -> Boolean = { _, _ -> false }
) {
    Row(
        modifier = modifier.padding(padding),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Center
    ) {
        for (star in 1..5) {
            Icon(
                imageVector = Default.Star,
                contentDescription = "star",
                modifier = Modifier
                    .width(size)
                    .height(size)
                    .pointerInteropFilter { onTouchEvent(star, it) }
                    .testTag(RATING_STAR + star),
                tint = if (star <= rating) colors.selectedStar else colors.unselectedStar
            )
        }
    }

}