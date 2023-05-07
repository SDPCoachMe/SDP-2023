package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a chat
 */
data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // emails of participants
    val messages: List<Message> = emptyList(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", emptyList(), emptyList())

    companion object {

        /**
         * Marks all messages in a list as read by the current user, except for the messages sent by the current user
         *
         * @param chat the chat whose messages to mark as read
         * @param currentUserEmail the email of the current user
         */
        fun markOtherUsersMessagesAsRead(chat: Chat, currentUserEmail: String): Chat {
            return chat.copy(messages = chat.messages.map { message ->
                if (message.readState == Message.ReadState.READ
                    || message.sender == currentUserEmail
                    || message.readByUsers.containsKey(currentUserEmail)) {

                    message
                } else {
                    val newReadByUsers = message.readByUsers.plus(Pair(currentUserEmail.replace(".", ","), true))
                    val newReadState = if (newReadByUsers.size == chat.participants.size - 1) Message.ReadState.READ else message.readState

                    println(message.content)
                    println("readstate: $newReadState")

                    message.copy(readState = newReadState, readByUsers = newReadByUsers)
                }
            })
        }
    }
}
