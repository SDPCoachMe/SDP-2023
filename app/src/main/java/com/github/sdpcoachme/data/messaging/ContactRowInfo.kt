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
)