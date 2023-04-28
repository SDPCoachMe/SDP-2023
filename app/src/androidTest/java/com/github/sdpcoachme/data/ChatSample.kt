package com.github.sdpcoachme.data

import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.ReadState
import java.time.LocalDateTime

class ChatSample {
    companion object {

        private val MESSAGE_1 = Message(
            sender = UserInfoSamples.COACH_1.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.READ
        )
        private val MESSAGE_2 = Message(
            sender = UserInfoSamples.COACH_2.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.READ
        )
        private val MESSAGE_3 = Message(
            sender = UserInfoSamples.COACH_1.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.RECEIVED
        )
        private val MESSAGE_4 = Message(
            sender = UserInfoSamples.COACH_2.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.RECEIVED
        )
        private val MESSAGE_5 = Message(
            sender = UserInfoSamples.COACH_2.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.SENT
        )
        private val MESSAGE_6 = Message(
            sender = UserInfoSamples.COACH_1.email,
            content = "Hello, I would like to book a session with you",
            timestamp = LocalDateTime.now().toString(),
            readState = ReadState.SENT
        )
        val MESSAGES = listOf(MESSAGE_1, MESSAGE_2, MESSAGE_3, MESSAGE_4, MESSAGE_5, MESSAGE_6)

        val CHAT = Chat(
            id = "default_chat_id",
            participants = listOf(MESSAGE_1.sender, MESSAGE_2.sender),
            messages = MESSAGES
        )
    }
}