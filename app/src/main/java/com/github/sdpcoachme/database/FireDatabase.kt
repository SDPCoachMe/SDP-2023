package com.github.sdpcoachme.database

import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Chat.Companion.markOtherUsersMessagesAsRead
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

    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return childExists(accounts, userID)
    }

    override fun addEvent(email: String, event: Event, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val id = email.replace('.', ',')
        return getSchedule(email, currentWeekMonday).thenCompose {
            val updatedSchedule = it.copy(events = it.events + event)  // Add new events to the schedule
            setChild(schedule, id, updatedSchedule).thenApply { updatedSchedule }// Update DB
        }
    }

    override fun addGroupEvent(groupEvent: GroupEvent, currentWeekMonday: LocalDate): CompletableFuture<Void> {
        var errorPreventionFuture = CompletableFuture<Void>()

        if (groupEvent.participants.size > groupEvent.maxParticipants) {
            errorPreventionFuture.completeExceptionally(Exception("Group event should not be full, initially"))
        } else if (groupEvent.participants.isEmpty()) {
            errorPreventionFuture.completeExceptionally(Exception("Group event must have at least 2 participants"))
        } else if (LocalDateTime.parse(groupEvent.event.start).isBefore(LocalDateTime.now())) {
            errorPreventionFuture.completeExceptionally(Exception("Group event cannot be in the past"))
        } else {
            errorPreventionFuture = setChild(groupEvents, groupEvent.groupEventId, groupEvent).thenCompose {
                registerForGroupEvent(groupEvent.groupEventId)
            }
        }

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
                        setChild(schedule, id, updatedSchedule) // Return updated schedule?
                    }
                }
            }
        }
    }

    override fun getSchedule(email: String, currentWeekMonday: LocalDate): CompletableFuture<Schedule> {
        val id = email.replace('.', ',')
        return getChild(schedule, id).thenApply { it.getValue(Schedule::class.java)!! }
            .exceptionally { Schedule() }
    }

    override fun getGroupEvent(groupEventId: String, currentWeekMonday: LocalDate): CompletableFuture<GroupEvent> {

        return getChild(groupEvents, groupEventId).thenApply { it.getValue(GroupEvent::class.java)!! }
            .exceptionally { GroupEvent() }
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        return getUser(email).thenApply {
            it.chatContacts
        }.thenCompose { list ->
            val mappedF = list.map { email ->
                getUser(email)
            }
            // done to make sure that all the futures are completed before calling join
            val allOf = CompletableFuture.allOf(*mappedF.toTypedArray())

            allOf.thenApply {
                mappedF.map { it.join() }
            }
        }
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        val id = chatId.replace('.', ',')
        return getChild(chats, id).thenApply { it.getValue(Chat::class.java)!! }
            .exceptionally { Chat() }
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
                setChild(chats, id, markOtherUsersMessagesAsRead(
                        chat,
                        email
                    )
                )
            } else {
                CompletableFuture.completedFuture(null)
            }
        }
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

