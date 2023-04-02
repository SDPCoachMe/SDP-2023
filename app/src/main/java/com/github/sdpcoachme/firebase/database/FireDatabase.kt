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

    // Cache for the users (write-through)
    private var usersCache = mutableMapOf<String, UserInfo>()
    private var usersLoaded = false

    override fun get(key: String): CompletableFuture<Any> {
        return getChild(rootDatabase, key).thenApply { it.value }
    }

    // todo big problem if we modify user through this method and then try to get it from the cache
    // temporary solution: clear the cache if we modify the user through this method
    override fun set(key: String, value: Any): CompletableFuture<Void> {
        usersCache.clear()
        usersLoaded = false
        return setChild(rootDatabase, key, value)
    }

    override fun addUser(user: UserInfo): CompletableFuture<Void> {
        val userID = user.email.replace('.', ',')
        return setChild(accounts, userID, user).thenAccept { usersCache[user.email] = user }
    }

    override fun getUser(email: String): CompletableFuture<UserInfo> {
        if (usersLoaded || usersCache.containsKey(email)) {
            return CompletableFuture.completedFuture(usersCache[email])
        }
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.getValue(UserInfo::class.java) }
    }

    override fun getAllUsers(): CompletableFuture<List<UserInfo>> {
        return getRef(accounts)
            .thenApply { snapshot ->
                snapshot.children.map { it.getValue(UserInfo::class.java)!! /* can't be null */ }
            }.thenApply { users ->
                usersLoaded = true
                usersCache = users.associateBy { it.email }.toMutableMap()
                users
            }
    }

    override fun userExists(email: String): CompletableFuture<Boolean> {
        if (usersCache.containsKey(email)) {
            return CompletableFuture.completedFuture(true)
        }
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.exists() }
    }

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {
        return getUser(email).thenAccept {
            val updatedUserInfo = it.copy(events = it.events + events)
            addUser(updatedUserInfo)
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

