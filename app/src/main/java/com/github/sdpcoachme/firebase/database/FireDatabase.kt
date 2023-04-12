package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
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
    private var currEmail = ""
    private val chats: DatabaseReference = rootDatabase.child("coachme").child("messages")
    var valueEventListener: ValueEventListener? = null

    override fun get(key: String): CompletableFuture<Any> {
        return getChild(rootDatabase, key).thenApply { it.value }
    }

    override fun updateUser(user: UserInfo): CompletableFuture<Void> {
        val userID = user.email.replace('.', ',')
        return setChild(accounts, userID, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.getValue(UserInfo::class.java) }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return getRef(accounts)
            .thenApply { snapshot ->
                snapshot.children.map { it.getValue(UserInfo::class.java)!! /* can't be null */ }
            }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.exists() }
    }

    override fun addEventsToUser(email: String, events: List<Event>): CompletableFuture<Void> {
        return getUser(email).thenCompose {
            val updatedUserInfo = it.copy(events = it.events + events)
            updateUser(updatedUserInfo)
        }
    }

    override fun getCurrentEmail(): String {
        return currEmail
    }

    override fun setCurrentEmail(email: String) {
        currEmail = email
    }

    override fun getChatContacts(email: String): CompletableFuture<List<UserInfo>> {
        return getUser(currentUserEmail).thenApply {
            it.chatContacts
        }.thenCompose { list ->
            val mappedF = list.map { email ->
                getUser(email) //TODO: how to change this to not use .get() ???
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
            val updatedChat = chat.copy(messages = chat.messages + message)
            setChild(chats, id, updatedChat)
        }
    }

    override fun markMessagesAsRead(chatId: String, email: String): CompletableFuture<Void> {
        val id = chatId.replace('.', ',')
        return getChat(id).thenCompose { chat ->
            val readBys = chat.messages.filter { it.sender != email }.map { it.readByRecipient }

            if (readBys.isNotEmpty() && readBys.contains(false)) { // check if update is needed
                val updatedMessages = chat.messages.map { message ->
                    if (message.sender != email) {
                        message.copy(readByRecipient = true)
                    } else {
                        message
                    }
                }
                setChild(chats, id, chat.copy(messages = updatedMessages))
            } else {
                CompletableFuture.completedFuture(null)
            }
        }
    }

    override fun addChatListener(chatId: String, onChange: (Chat) -> Unit) {
        println("Adding listener!!!")
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
            println("Removing listener!!!")
            chatRef.removeEventListener(valueEventListener!!)
        }
    }


    /**
     * Sets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to set the key-value pair
     * @param key the key of the value
     * @param value the value to set
     * @return a completable future that completes when the child is set
     */
    private fun setChild(databaseChild: DatabaseReference, key: String, value: Any): CompletableFuture<Void> {
        val ref = databaseChild.child(key)
        return setRef(ref, value)
    }

    /**
     * Gets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to get the key-value pair
     * @param key the key of the value
     * @return a completable future that completes when the child is set. If the key does not exist,
     * the future completes exceptionally with a NoSuchKeyException.
     */
    private fun getChild(databaseChild: DatabaseReference, key: String): CompletableFuture<DataSnapshot> {
        val ref = databaseChild.child(key)
        return getRef(ref)
    }

    /**
     * Sets a value in a given database reference
     * @param databaseRef the database reference in which to set the value
     * @param value the value to set
     * @return a completable future that completes when the child is set
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
     * Gets a value in a given database reference
     * @param databaseRef the database reference in which to get the value
     * @return a completable future that completes when the child is set. If the key does not exist,
     * the future completes exceptionally with a NoSuchKeyException.
     */
    private fun getRef(databaseRef: DatabaseReference): CompletableFuture<DataSnapshot> {
        val future = CompletableFuture<DataSnapshot>()
        databaseRef.get().addOnSuccessListener {
            if (it.value == null) future.completeExceptionally(NoSuchKeyException())
            else future.complete(it)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }
}

