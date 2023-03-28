package com.github.sdpcoachme.firebase.database

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
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


    override fun userExists(email: String): CompletableFuture<Boolean> {
        val userID = email.replace('.', ',')
        return getChild(accounts, userID).thenApply { it.exists() }
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

    //Damian's stuff
    override fun getAccountsRef(): DatabaseReference {
        return accounts
    }

    override fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void> {

        return this.getUser(email).thenAccept {
            // TODO: remove this try catch block
            try {
                val formattedEmail = email.replace('.', ',')

                val eventsToAdd = hashMapOf<String, Any>()
                for (event in events) {

                    val eventMap = hashMapOf<String, Any>(
                        "name" to event.name,
                        "color" to event.color.toArgb(),
                        "start" to event.start.toString(),
                        "end" to event.end.toString(),
                        "description" to event.description,
                    )
                    val eventsKey = this.getAccountsRef().child(formattedEmail).child("events").push().key
                    eventsToAdd["/$eventsKey"] = eventMap
                }
                this.getAccountsRef().child(formattedEmail).child("events").updateChildren(eventsToAdd)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

