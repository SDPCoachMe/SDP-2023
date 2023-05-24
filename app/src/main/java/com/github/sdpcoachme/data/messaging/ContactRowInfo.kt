package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a row in the contact list
 * Can be either a group chat or a single chat (with a single recipient)
 */
data class ContactRowInfo (
    val chatId: String = "", // recipient id or group chat id
    val chatTitle: String = "", // name of recipient / group chat name
    val lastMessage: Message = Message(),
    val isGroupChat: Boolean = false,
    val participants: List<String> = emptyList(), // emails of participants
    // Note: participants may be an empty list even if the chat has participants! It is only used in
    // CoachesListActivity for practical reasons, and temporarily until Chat and ContactRowInfo are merged
    // TODO: this class should be merged with Chat as it is redundant
)