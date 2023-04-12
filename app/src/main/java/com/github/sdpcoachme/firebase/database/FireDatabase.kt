package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.util.concurrent.CompletableFuture

/**
 * A database class that uses Firebase
 */
class FireDatabase(databaseReference: DatabaseReference) : Database {

    private val rootDatabase: DatabaseReference = databaseReference
    private val accounts: DatabaseReference = rootDatabase.child("coachme").child("accounts")
    private var currEmail = ""

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
            users.values.map { it.getValue(UserInfo::class.java)!! /* can't be null */ }
        }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        // TODO: this does not work
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

    /**
     * Gets all children of a given database reference
     * @param databaseRef the database reference whose children to get
     * @return a completable future that completes when the children are retrieved. The future
     * contains a map of the children, with the key being the key of the child and the value being
     * the child itself
     */
    private fun getAllChildren(databaseRef: DatabaseReference): CompletableFuture<Map<String, DataSnapshot>> {
        return getRef(databaseRef).thenApply {
            return it.children.associateBy { child -> child.key!! /* can't be null */ })
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
     * Sets the value of a given database reference
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
     * Gets the value of a given database reference
     * @param databaseRef the database reference in which to get the value
     * @return a completable future that completes when the child is set. If the reference path does
     * not exist in the database, the future completes exceptionally with a NoSuchKeyException.
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

