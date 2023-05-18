package com.github.sdpcoachme.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    image: ImageData? = null,
    title: String,
    titleTag: String? = null,
    firstRow: (@Composable () -> Unit)? = null,
    secondRow: (@Composable () -> Unit)? = null,
    secondColumn: (@Composable () -> Unit)? = null,
    firstColumnMaxWidth : Float = 1f,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            if (onClick != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClick()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(100.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(100.dp)
           },
        verticalAlignment = Alignment.CenterVertically
    ) {
        image?.let {
            Image(
                painter = image.painter,
                contentDescription = image.contentDescription,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(firstColumnMaxWidth).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                modifier = titleTag?.let { Modifier.testTag(titleTag) } ?: Modifier,
                text = title,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            firstRow?.let {
                firstRow()
            }
            secondRow?.let {
                secondRow()
            }
        }
        secondColumn?.let {
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {
                secondColumn()
            }
        }
    }
    Divider()
}

@Composable
fun IconTextRow(
    icon: IconData,
    text: String,
    textTag: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon.icon,
            tint = Color.Gray,
            contentDescription = icon.contentDescription,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Temporary, until we implement proper location handling
        Text(
            modifier = textTag?.let { Modifier.testTag(textTag) } ?: Modifier,
            text = text,
            color = Color.Gray,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IconsRow(
    icons: List<IconData>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEach {
            Icon(
                imageVector = it.icon,
                tint = Color.Gray,
                contentDescription = it.contentDescription,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun Label(
    text: String,
    textTag: String? = null,
    icon: IconData? = null,
    backgroundColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = icon.icon,
                tint = contentColor,
                contentDescription = icon.contentDescription,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            modifier = textTag?.let { Modifier.testTag(textTag) } ?: Modifier,
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.overline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class IconData(
    val icon: ImageVector,
    val contentDescription: String
)

data class ImageData(
    val painter: Painter,
    val contentDescription: String
)