package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Chat.Companion.markOtherUsersMessagesAsRead
import com.github.sdpcoachme.data.messaging.ContactRowInfo
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.data.messaging.Message.*
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.schedule.EventOps
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * A database class that uses Firebase
 */
class FireDatabase(databaseReference: DatabaseReference) : Database {

    private val rootDatabase: DatabaseReference = databaseReference
    private val accounts: DatabaseReference = rootDatabase.child("coachme").child("accounts")
    private var currEmail = ""
    private val chats: DatabaseReference = rootDatabase.child("coachme").child("messages")
    private val fcmTokens: DatabaseReference = rootDatabase.child("coachme").child("fcmTokens")
    private val schedule: DatabaseReference = rootDatabase.child("coachme").child("schedule")
    private val groupEvents: DatabaseReference = schedule.child("groupEvents")
    var valueEventListener: ValueEventListener? = null

    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        val userID = user.email.replace('.', ',')
        return setChild(accounts, userID, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.getValue(UserInfo::class.java) }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return getAllChildren(accounts).thenApply { users ->
            users.values.map {
                try { // done to ensure that erroneous users in the
                    // db do not inhibit the other users to be retrieved
                    it.getValue(UserInfo::class.java)!!
                } catch (e: Exception) {
                    UserInfo()
                }
            }.filter { it != UserInfo() }
        }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return childExists(accounts, userID)
    }

    override fun addEvent(event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val id = currEmail.replace('.', ',')
        return getSchedule(currentWeekMonday).thenCompose {
            val updatedSchedule = it.copy(events = it.events + event)  // Add new events to the schedule
            setChild(schedule, id, updatedSchedule).thenApply { updatedSchedule }// Update DB
        }
    }

    override fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void> {
        var errorPreventionFuture = CompletableFuture<Void>()
//        return errorPreventionFuture.thenAccept {
            if (groupEvent.participants.size > groupEvent.maxParticipants) {
                errorPreventionFuture.completeExceptionally(Exception("Group event should not be full, initially"))
            } else if (groupEvent.participants.size < 2) {
                errorPreventionFuture.completeExceptionally(Exception("Group event must have at least 2 participants"))
            } else if (LocalDateTime.parse(groupEvent.event.start).isBefore(LocalDateTime.now())) {
                errorPreventionFuture.completeExceptionally(Exception("Group event cannot be in the past"))
            } else {
//                errorPreventionFuture.complete(null)
                errorPreventionFuture = setChild(groupEvents, groupEvent.groupEventId, groupEvent)
            }
//        }.thenCompose {
//        }
        return errorPreventionFuture
    }

    override fun registerForGroupEvent(groupEventId: String): CompletableFuture<Void> {
        val id = currEmail.replace('.', ',')
        return getGroupEvent(groupEventId, EventOps.getStartMonday()).thenCompose { groupEvent ->
            val hasCapacity = groupEvent.participants.size < groupEvent.maxParticipants
            if (!hasCapacity) {
                val failingFuture = CompletableFuture<Void>()
                failingFuture.completeExceptionally(Exception("Group event is full"))
                failingFuture
            } else {
                val updatedGroupEvent = groupEvent.copy(participants = groupEvent.participants + currEmail)
                setChild(groupEvents, groupEventId, updatedGroupEvent).thenCompose {
                    getSchedule(EventOps.getStartMonday()).thenCompose { s ->
                        val updatedSchedule = s.copy(groupEvents = s.groupEvents + groupEventId)
                        setChild(schedule, id, updatedSchedule)
                    }
                }
            }
        }
    }

    override fun getSchedule(currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val id = currEmail.replace('.', ',')
        return getChild(schedule, id).thenApply { it.getValue(Schedule::class.java)!! }
            .exceptionally {
                Schedule() }
    }

    override fun getGroupEvent(groupEventId: String, currentWeekMonday: LocalDate): CompletableFuture<GroupEvent> {
        return getChild(groupEvents, groupEventId).thenApply { it.getValue(GroupEvent::class.java)!! }
            .exceptionally { GroupEvent() }
    }

    override fun getCurrentEmail(): String {
        return currEmail
    }

    override fun setCurrentEmail(email: String) {
        currEmail = email
    }

    override fun getContactRowInfo(email: String): CompletableFuture<List<ContactRowInfo>> {
        return getUser(currEmail).thenApply {
            println("user: $it")
            it.chatContacts.filterNotNull()

        }.thenCompose { contactList ->
            println("contactList: $contactList")
            val mappedF = contactList.map { contactId ->
                val isGroupChat = contactId.startsWith("@@event")
                val chatId = if (isGroupChat) contactId
                else {
                    if (contactId < currEmail) contactId + currEmail
                    else currEmail + contactId
                }

                getChat(chatId).thenCompose { chat ->
                    val lastMessage = if (chat.messages.isEmpty()) Message() else chat.messages.last()

                    if (contactId.startsWith("@@event")) {
                        // TODO: get event info here!!!
                        //  getEventInfo(contactId)
                        getGroupEvent(contactId, EventOps.getStartMonday().plusDays(7))
                            .thenApply { groupEvent ->
                                val row = ContactRowInfo(
                                    chatId = chatId,
                                    chatTitle = groupEvent.event.name,
                                    lastMessage = lastMessage,
                                    isGroupChat = true
                                )
                                row
                            }.exceptionally { println("error in getgroup event") ; null}
//                        CompletableFuture.completedFuture(
//                            ContactRowInfo(
//                                chatId = chatId,
//                                chatTitle = "TEST EVENT",
//                                lastMessage = lastMessage,
//                                isGroupChat = true
//                            )
//                        )
                    } else {
                        getUser(contactId).thenApply {
                            val row = ContactRowInfo(
                                chatId = chatId,
                                chatTitle = "${it.firstName} ${it.lastName}",
                                lastMessage = lastMessage,
                                isGroupChat = false
                            )
                            row
                        }.exceptionally { println("error in getuser") ; println(it.cause); null}
                    }
                }.exceptionally { println("error in get chat") ; null}
            }
            // done to make sure that all the futures are completed before calling join
            val allOf = CompletableFuture.allOf(*mappedF.toTypedArray())
                .exceptionally { println("error in all of") ; null }

            allOf.thenApply {
                mappedF.map { it.join() }
            }
        }.exceptionally { println("error in get contact row info") ; println(it.cause); null}
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
                println("marking as read...")
                val newChat = markOtherUsersMessagesAsRead(
                    chat,
                    email
                )
                println("newChat: $newChat")
                setChild(chats, id, newChat
                ).exceptionally { println("exception... in set child") ; null}
            } else {
                CompletableFuture.completedFuture(null)
            }
        }.exceptionally { println("exception... in get chat of mark messages as read") ; println(it.cause); null}
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
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

    /**
     * Gets all children of a given database reference
     * @param databaseRef the database reference whose children to get
     * @return a completable future that completes when the children are retrieved. The future
     * contains a map of the children, with the key being the key of the child and the value being
     * the child itself
     */
    private fun getAllChildren(databaseRef: DatabaseReference): CompletableFuture<Map<String, DataSnapshot>> {
        return getRef(databaseRef).thenApply {
            it.children.associateBy { child -> child.key!! /* can't be null */ }
        }
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

