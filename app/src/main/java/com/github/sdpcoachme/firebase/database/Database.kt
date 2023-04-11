package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.UserInfo
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.concurrent.CompletableFuture

/**
 * A database interface
 */
interface Database {

    /**
     * Get a value from the database
     * @param key The key of the value to get
     * @return A future that will complete with the value
     */
    fun get(key: String): CompletableFuture<Any>

    /**
     * Set a value in the database
     * @param key The key of the value to set
     * @param value The value to set
     * @return A future that will complete when the value has been set
     */
    fun set(key: String, value: Any): CompletableFuture<Void>

    /**
     * Add a user to the database
     * @param user The user to add
     * @return A future that will complete when the user has been added
     */
    // TODO change the name since this can also be used to update a user
    fun addUser(user: UserInfo): CompletableFuture<Void>

    /**
     * Get a user from the database
     * @param email The email of the user to get
     * @return A future that will complete when the user has been gotten
     */
    fun getUser(email: String): CompletableFuture<UserInfo>

    /**
     * Get all users from the database
     * @return A future that will complete with a list of all users in the database
     */
    fun getAllUsers(): CompletableFuture<List<UserInfo>>

    /**
     * Get all users from the database sorted by distance from the current location
     * @param currentLatitude The latitude of the current location
     * @param currentLongitude The longitude of the current location
     * @return A future that will complete with a list of all users in the database sorted by distance
     */
    fun getAllUsersByNearest(currentLatitude: Double, currentLongitude: Double): CompletableFuture<List<UserInfo>> {
        return getAllUsers().thenApply { users ->
            users.sortedBy { user ->
                val userLatitude = user.location.latitude
                val userLongitude = user.location.longitude
                val distance = SphericalUtil.computeDistanceBetween(
                    LatLng(currentLatitude, currentLongitude),
                    LatLng(userLatitude, userLongitude)
                )
                distance
            }
        }
    }

    /**
     * Check if a user exists in the database
     * @param email The email of the user to check
     * @return A future that will complete when the user has been checked
     */
    fun userExists(email: String): CompletableFuture<Boolean>

    /**
     * Add events to the database
     * @param email The email of the user to add the events to
     * @param events The events to add
     * @return A future that will complete when the events have been added
     */
    fun addEventsToDatabase(email: String, events: List<Event>): CompletableFuture<Void>

}