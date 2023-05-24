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

/**
 * A generic list item that can be used in a vertical list.
 *
 * @param image the image to display on the left side of the list item
 * @param title the title to display on the list item
 * @param titleTag the tag to use for the title
 * @param firstRow the first row to display below the title
 * @param secondRow the second row to display below the first row
 * @param secondColumn the column to display on the right side of the list item
 * @param firstColumnMaxWidth the max width of the first column (includes title, first row, and second row)
 * @param onClick the action to perform when the list item is clicked
 */
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

/**
 * A generic small list item that can be used in a vertical list.
 *
 * @param image the image to display on the left side of the list item
 * @param title the title to display on the list item
 * @param titleTag the tag to use for the title
 * @param onClick the action to perform when the list item is clicked
 */
@Composable
fun SmallListItem(
    image: ImageData? = null,
    title: String,
    titleTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
            .padding(10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        image?.let {
            Image(
                painter = image.painter,
                contentDescription = image.contentDescription,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
                    .padding(0.dp, 0.dp, 0.dp, 0.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            modifier = titleTag?.let { Modifier.testTag(titleTag) } ?: Modifier,
            text = title,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Divider()
}

/**
 * This composable can be used as a row in a list item, containing an icon and text. The icon is
 * optional.
 *
 * @param icon the icon displayed on the left side of the row
 * @param text the text displayed next to the icon
 * @param maxLines the max number of lines to display for the text
 * @param textTag the tag to use for the text
 */
@Composable
fun IconTextRow(
    icon: IconData? = null,
    text: String,
    maxLines: Int = 1,
    textTag: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = icon.icon,
                tint = Color.Gray,
                contentDescription = icon.contentDescription,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        // Temporary, until we implement proper location handling
        Text(
            modifier = textTag?.let { Modifier.testTag(textTag) } ?: Modifier,
            text = text,
            color = Color.Gray,
            style = MaterialTheme.typography.body2,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * This composable can be used as a row in a list item, containing a list of icons.
 *
 * @param icons the list of icons to display
 */
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

/**
 * This composable is used to display a label in a list item. The label contains text and an optional
 * icon.
 *
 * @param text the text to display in the label
 * @param textTag the tag to use for the text
 * @param icon the icon to display in the label
 * @param backgroundColor the background color of the label
 * @param contentColor the color of the text and icon in the label
 */
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

/**
 * Represents an icon and its content description. Used to more easily pass icons as parameters.
 */
data class IconData(
    val icon: ImageVector,
    val contentDescription: String
)

/**
 * Represents an image and its content description. Used to more easily pass images as parameters.
 */
data class ImageData(
    val painter: Painter,
    val contentDescription: String
)