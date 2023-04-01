package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.data.messaging.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.util.concurrent.CompletableFuture

/**
 * A database class that uses Firebase
 */
class FireDatabase(databaseReference: DatabaseReference) : Database {

    private val rootDatabase: DatabaseReference = databaseReference
    private val accounts: DatabaseReference = rootDatabase.child("coachme").child("accounts")

    override fun get(key: String): CompletableFuture<Any> {
        return getChild(rootDatabase, key).thenApply { it.value }
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        return setChild(rootDatabase, key, value)
    }

    override fun addUser(user: UserInfo): CompletableFuture<Void> {
        val userID = user.email.replace('.', ',')
        return setChild(accounts, userID, user)
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.getValue(UserInfo::class.java) }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        // TODO: might need refactoring to be more modular
        val future = CompletableFuture<List<UserInfo>>()
        accounts.get().addOnSuccessListener { snapshot ->
            val users = snapshot.children.map { it.getValue(UserInfo::class.java)!! /* can't be null */ }
            future.complete(users)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.exists() }
    }

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {
        return this.getUser(email).thenAccept {
            val updatedUserInfo = it.copy(events = it.events + events)
            addUser(updatedUserInfo)
        }
    }

    override fun getChat(chatId: String): CompletableFuture<Chat> {
        // TODO: implement
        val chat = Chat().copy(
            id = chatId,
            messages = listOf(
                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("lucaengu@gmail.com", "How are you? ------------------- ----------------------------------------------------------------------------------------------------------"),

                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("lucaengu@gmail.com", "How are you? ------------------- ----------------------------------------------------------------------------------------------------------"),

                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("lucaengu@gmail.com", "How are you? ------------------- ----------------------------------------------------------------------------------------------------------"),

                Message("luca.aengu@gmail.com", "Hello ----------------------------- -----------------------------------------------------------------------------------------"),
                Message("lucaengu@gmail.com", "How are you? ------------------- ----------------------------------------------------------------------------------------------------------")
            )
        )
        return CompletableFuture.completedFuture(chat)
    }

    override fun sendMessage(chatId: String, message: Message): CompletableFuture<Void> {
        // TODO: implement
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Sets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to set the key-value pair
     * @param key the key of the value
     * @param value the value to set
     * @return a completable future that completes when the child is set
     */
    private fun setChild(databaseChild: DatabaseReference, key: String, value: Any): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        databaseChild.child(key).setValue(value).addOnSuccessListener {
            future.complete(null)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }

    /**
     * Gets a key-value pair with a given key in a given database reference
     * @param databaseChild the database reference in which to get the key-value pair
     * @param key the key of the value
     * @return a completable future that completes when the child is set. If the key does not exist,
     * the future completes exceptionally with a NoSuchKeyException.
     */
    private fun getChild(databaseChild: DatabaseReference, key: String): CompletableFuture<DataSnapshot> {
        val future = CompletableFuture<DataSnapshot>()
        databaseChild.child(key).get().addOnSuccessListener {
            if (it.value == null) future.completeExceptionally(NoSuchKeyException())
            else future.complete(it)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }
}

