package com.github.sdpcoachme.data.messaging

import com.github.sdpcoachme.data.UserInfo

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

    /**
     * Returns the resource id for the profile picture of the chat. Note that this is temporary,
     * and will be replaced by a real profile picture in a future version. For now, this functions
     * hashes the chat's id and returns one of the predefined profile pictures located in the
     * drawable folder.
     */
    // TODO: this method is temporary and a lot of its content is not ideally implemented. It will be
    //  fixed when we implement real profile pictures
    fun getChatPictureResource(currentUserEmail: String): Int {
        if (id.startsWith("@@event")) {
            // TODO: code similar to the one in UserInfo.getProfilePictureResource(String)
            val prefix = "chat_picture_"
            val fieldNames = com.github.sdpcoachme.R.drawable::class.java.fields.filter {
                it.name.startsWith(prefix)
            }
            // mod returns same sign as divisor, so no need to use abs
            val index = id.hashCode().mod(fieldNames.size)
            val field = fieldNames[index]

            return field.get(null) as Int
        } else {
            // participants size must be 2 here
            val otherUserEmail = participants.filterNot {
                it == currentUserEmail
            }.first()

            return UserInfo.getProfilePictureResource(otherUserEmail)
        }
    }

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

                    message.copy(readState = newReadState, readByUsers = newReadByUsers)
                }
            })
        }

        /**
         * Get the chat id for a personal chat between two users
         *
         * @param email1 The email of the first user
         * @param email2 The email of the second user
         * @return The chat id
         */
        fun chatIdForPersonalChats(email1: String, email2: String): String {
            return if (email1 < email2) "$email1$email2" else "$email2$email1"
        }
    }
}
