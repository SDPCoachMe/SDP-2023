package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.ChatSample.Companion.CHAT
import com.github.sdpcoachme.data.messaging.ReadState
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class FireDatabaseTest {

    @Test
    fun markOtherUsersMessagesAsReadMarksMessagesFromOtherUserAsRead() {
        FireDatabase.markOtherUsersMessagesAsRead(CHAT, CHAT.participants[0]).messages
            .forEach { msg ->
                if (msg.sender != CHAT.participants[0]) {
                    assertThat(msg.readState, `is`(ReadState.READ))
                }
            }
    }
}