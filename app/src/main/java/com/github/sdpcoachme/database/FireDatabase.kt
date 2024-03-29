package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Chat.Companion.markOtherUsersMessagesAsRead
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.ReadState
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CompletableFuture

/**
 * A database class that uses Firebase
 */
class FireDatabase(databaseReference: DatabaseReference) : Database {

    private val rootDatabase: DatabaseReference = databaseReference
    private val accounts: DatabaseReference = rootDatabase.child("coachme").child("accounts")
    private val chats: DatabaseReference = rootDatabase.child("coachme").child("messages")
    private val fcmTokens: DatabaseReference = rootDatabase.child("coachme").child("fcmTokens")
    private val schedule: DatabaseReference = rootDatabase.child("coachme").child("schedule")
    private val groupEvents: DatabaseReference = rootDatabase.child("coachme").child("groupEvents")
    private var valueEventListener: ValueEventListener? = null

    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        val userID = user.email.replace('.', ',')
        return setChild(accounts, userID, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.getValue(UserInfo::class.java) }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return childExists(accounts, userID)
    }

    override fun getGroupEvent(groupEventId: String): CompletableFuture<GroupEvent> {
        val id = groupEventId.replace('.', ',')
        return getChild(groupEvents, id).thenApply { it.getValue(GroupEvent::class.java)!! }
            .exceptionally { GroupEvent() }
    }

    override fun getAllGroupEvents(): CompletableFuture<List<GroupEvent>> {
        return getRef(groupEvents)
            .thenApply { getAllChildren(it) }
            .thenApply { events -> mapDStoList(events.values, GroupEvent::class.java) }
    }

    override fun updateGroupEvent(groupEvent: GroupEvent): CompletableFuture<Void> {
        val id = groupEvent.groupEventId.replace('.', ',')
        return setChild(groupEvents, id, groupEvent)
    }

    override fun addGroupEventToSchedule(email: String, groupEventId: String): CompletableFuture<Schedule> {
        val id = email.replace('.', ',')

        return getSchedule(email).thenCompose { s ->
            getGroupEvent(groupEventId).thenCompose { groupEvent ->
                val updatedSchedule = s.copy(
                    events = s.events + EventOps.groupEventsToEvents(listOf(groupEvent)),
                    groupEvents = s.groupEvents + groupEventId
                )
                setChild(schedule, id, updatedSchedule).thenApply { updatedSchedule }
            }
        }
    }

    override fun addEventToSchedule(email: String, event: Event): CompletableFuture<Schedule> {
        val id = email.replace('.', ',')
        return getSchedule(email).thenCompose {
            val updatedSchedule = it.copy(events = it.events + event)  // Add new events to the schedule
            setChild(schedule, id, updatedSchedule).thenApply { updatedSchedule }// Update DB
        }
    }

    override fun getSchedule(email: String): CompletableFuture<Schedule> {
        val id = email.replace('.', ',')
        return getChild(schedule, id).thenApply { it.getValue(Schedule::class.java)!! }
            .exceptionally { Schedule() }
    }

    override fun getContactRowInfos(email: String): CompletableFuture<List<ContactRowInfo>> {
        return getUser(email).thenApply {
            // this line has been added to prevent strange buts where the chatContacts list
            // contains null values (which should not be possible according to AndroidStudio but can happen)
            it.chatContacts.filterNotNull()
        }.thenCompose { contactList ->

            val mappedF = contactList.map { contactId ->
                val isGroupChat = contactId.startsWith("@@event")
                val chatId = if (isGroupChat) contactId
                else { // since, here, the contactId is the email of the recipient
                    Chat.chatIdForPersonalChats(email, contactId)
                    if (contactId < email) contactId + email
                    else email + contactId
                }

                // Fetch the chat and create the corresponding ContactRowInfo
                getContactRowInfoFromChat(chatId, contactId)
            }
            // done to make sure that all the futures are completed before calling join
            val allOf = CompletableFuture.allOf(*mappedF.toTypedArray())
                .exceptionally { println("error in all of") ; null }

            allOf.thenApply {
                mappedF.map { it.join() }
            }
        }.exceptionally { println("error in get contact row info") ; println(it.cause); null}
    }

    private fun getContactRowInfoFromChat(
        chatId: String,
        contactId: String
    ): CompletableFuture<ContactRowInfo> {

        return getChat(chatId).thenCompose { chat ->
            val lastMessage = chat.messages.lastOrNull() ?: Message()

            if (contactId.startsWith("@@event")) {
                getGroupEvent(contactId)
                    .thenApply { groupEvent ->
                        ContactRowInfo(
                            chatId = chatId,
                            chatTitle = groupEvent.event.name,
                            lastMessage = lastMessage,
                            isGroupChat = true
                        )
                    }
            } else {
                getUser(contactId).thenApply {
                    ContactRowInfo(
                        chatId = chatId,
                        chatTitle = "${it.firstName} ${it.lastName}",
                        lastMessage = lastMessage,
                        isGroupChat = false
                    )
                }
            }
        }
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        val id = chatId.replace('.', ',')
        return getChild(chats, id).thenApply { it.getValue(Chat::class.java)!! }
            .exceptionally { Chat() }
    }

    override fun updateChatParticipants(chatId: String, participants: List<String>): CompletableFuture<Void> {
        val id = chatId.replace('.', ',')
        return getChat(id).thenCompose { chat ->
            val updatedChat = chat.copy(id = chatId, participants = participants)
            setChild(chats, id, updatedChat)
        }
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        val id = chatId.replace('.', ',')
        return getChat(id).thenCompose { chat ->
            val updatedChat = chat.copy(id = chatId, messages = chat.messages + message)
            setChild(chats, id, updatedChat)
        }
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        val id = chatId.replace('.', ',')
        return getChat(id).thenCompose { chat ->
            val readStates = chat.messages.filter { it.sender != email }.map { it.readState }

            if (!readStates.all { it == ReadState.READ }) { // check if update is needed
                val newChat = markOtherUsersMessagesAsRead(
                    chat,
                    email
                )
                setChild(chats, id, newChat
                ).exceptionally { println("exception... in set child") ; null}
            } else {
                CompletableFuture.completedFuture(null)
            }
        }.exceptionally { println("exception... in get chat of mark messages as read") ; println(it.cause); null}
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        // make sure we don't have 2 listeners for the same chat
        removeChatListener(chatId)

        val id = chatId.replace('.', ',')
        val chatRef = chats.child(id)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val chat = dataSnapshot.getValue(Chat::class.java)
                chat?.let { onChange(it) }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
            }
        }

        this.valueEventListener = valueEventListener
        chatRef.addValueEventListener(valueEventListener)
    }

    override fun addUsersListeners(onChange: (List<UserInfo>) -> Unit) {
        val usersRef = accounts
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usersSnapShots = getAllChildren(dataSnapshot)
                val users = mapDStoList(usersSnapShots.values, UserInfo::class.java)
                onChange(users)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
            }
        }
        usersRef.addValueEventListener(valueEventListener)
    }

    override fun removeChatListener(chatId: String) {
        val id = chatId.replace('.', ',')
        val chatRef = chats.child(id)
        if (valueEventListener != null) {
            chatRef.removeEventListener(valueEventListener!!)
        }
    }

    override fun getFCMToken(email: String): CompletableFuture<String> {
        val userID = email.replace('.', ',')
        return getChild(fcmTokens, userID).thenApply { it.getValue(String::class.java)!! }
    }

    override fun setFCMToken(email: String, token: String): CompletableFuture<Void> {
        val userID = email.replace('.', ',')
        return setChild(fcmTokens, userID, token)
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return getRef(accounts)
            .thenApply { getAllChildren(it) }
            .thenApply { users -> mapDStoList(users.values, UserInfo::class.java)
        }
    }

    private fun <T> mapDStoList(ds: Collection<DataSnapshot>, clazz: Class<T>): List<T> {
        return ds.mapNotNull {
            try { // done to ensure that erroneous users in the
                // db do not inhibit the other users to be retrieved
                it.getValue(clazz)!!
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Gets all children of a Datasnapshot
     * @param dataSnapshot the Datasnapshot from which to get the children
     * @return a map of the children, with the key being the key of the child
     */
    private fun getAllChildren(dataSnapshot: DataSnapshot): Map<String, DataSnapshot> {
        return dataSnapshot.children.associateBy { child -> child.key!! } /* can't be null */
    }

    /**
     * Sets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to set the key-value pair
     * @param key the key of the value
     * @param value the value to set
     * @return a completable future that completes when the child is set
     * @throws IllegalArgumentException if the key is empty (no child can have an empty key)
     */
    private fun setChild(databaseChild: DatabaseReference, key: String, value: Any): CompletableFuture<Void> {
        // Careful here, giving an empty string as a key will return a reference to the parent!!
        if (key.isEmpty())
            throw IllegalArgumentException("Key cannot be empty")
        val ref = databaseChild.child(key)
        return setRef(ref, value)
    }

    /**
     * Gets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to get the key-value pair
     * @param key the key of the value
     * @return a completable future that completes when the child is set. If the key does not exist,
     * the future completes exceptionally with a NoSuchKeyException.
     * @throws IllegalArgumentException if the key is empty (no child can have an empty key)
     */
    private fun getChild(databaseChild: DatabaseReference, key: String): CompletableFuture<DataSnapshot> {
        // Careful here, giving an empty string as a key will return a reference to the parent!!
        if (key.isEmpty())
            throw IllegalArgumentException("Key cannot be empty")
        val ref = databaseChild.child(key)
        return getRef(ref)
    }

    /**
     * Checks if a child with a given key exists in a given database reference
     * @param databaseChild the database reference in which to check for the existence of the child
     * @param key the key of the child
     * @return a completable future that completes with true if the child exists, false otherwise
     * @throws IllegalArgumentException if the key is empty (no child can have an empty key)
     */
    private fun childExists(databaseChild: DatabaseReference, key: String): CompletableFuture<Boolean> {
        // Careful here, giving an empty string as a key will return a reference to the parent!!
        if (key.isEmpty())
            throw IllegalArgumentException("Key cannot be empty")
        val ref = databaseChild.child(key)
        return refExists(ref)
    }

    /**
     * Sets the value of a given database reference
     * @param databaseRef the database reference in which to set the value
     * @param value the value to set
     * @return a completable future that completes when the value is set
     */
    private fun setRef(databaseRef: DatabaseReference, value: Any): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        databaseRef.setValue(value).addOnSuccessListener {
            future.complete(null)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }

    /**
     * Gets the value of a given database reference
     * @param databaseRef the database reference for which to get the value
     * @return a completable future that completes with the value of the database reference. If the
     * reference path does not exist in the database, the future completes exceptionally with a
     * NoSuchKeyException.
     */
    private fun getRef(databaseRef: DatabaseReference): CompletableFuture<DataSnapshot> {
        val future = CompletableFuture<DataSnapshot>()
        databaseRef.get().addOnSuccessListener {
            if (!it.exists()) future.completeExceptionally(Database.NoSuchKeyException())
            else future.complete(it)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }

    /**
     * Checks if a given database reference exists
     * @param databaseRef the database reference to check
     * @return a completable future that completes with true if the reference exists, false otherwise
     */
    private fun refExists(databaseRef: DatabaseReference): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        databaseRef.get().addOnSuccessListener {
            future.complete(it.exists())
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }
}

