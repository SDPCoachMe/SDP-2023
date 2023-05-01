package com.github.sdpcoachme.data

import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class ChatTest {
    @Test
    fun markOtherUsersMessagesAsReadMarksMessagesFromOtherUserAsRead() {
        Chat.markOtherUsersMessagesAsRead(ChatSample.CHAT, ChatSample.CHAT.participants[0]).messages
            .forEach { msg ->
                if (msg.sender != ChatSample.CHAT.participants[0]) {
                    MatcherAssert.assertThat(
                        msg.readState,
                        CoreMatchers.`is`(Message.ReadState.READ)
                    )
                }
            }
    }
}