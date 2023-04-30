package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a message inside a chat
 */
data class Message(
    val sender: String = "",
    val content: String = "",
    val timestamp: String = "",
    val readState: ReadState = ReadState.SENT,
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", ReadState.SENT)

    /**
     * Enum class representing the read state of a message
     */
    enum class ReadState {
        SENT,
        RECEIVED,
        READ,
    }
}
