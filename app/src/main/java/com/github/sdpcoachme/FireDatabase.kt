package com.github.sdpcoachme

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.CompletableFuture

class FireDatabase<V>: Database<V> {

    private val db: DatabaseReference = Firebase.database.reference


    override fun get(key: String): CompletableFuture<V> {
        val future = CompletableFuture<V>()

        db.child(key).get().addOnSuccessListener {
            if (it.value == null) future.completeExceptionally(NoSuchFieldException())
            else future.complete(it.value as V?)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }

        return future
    }

    override fun set(key: String, value: V): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        db.child(key).setValue(value).addOnSuccessListener {
            future.complete(null)
        }.addOnFailureListener {
            future.completeExceptionally(it)
        }
        return future
    }
}

