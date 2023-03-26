package com.github.sdpcoachme.data

/**
 * A data class for a list item
 */
data class ListItem<A>(
    val element: A,
    val selected: Boolean
)
