package com.github.sdpcoachme

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.CompletableFuture

/**
 * A database class that uses Firebase
 */
class FireDatabase : Database {

    private val db: DatabaseReference = Firebase.database.reference

    override fun get(key: String): CompletableFuture<Any> {
        val future = CompletableFuture<Any>()

        db.child(key).get().addOnSuccessListener {
            if (it.value == null) future.completeExceptionally(NoSuchFieldException())
            else future.complete(it.value)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }

        return future
    }

    override fun set(key: String, value: Any): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        db.child(key).setValue(value).addOnSuccessListener {
            future.complete(null)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }
}

