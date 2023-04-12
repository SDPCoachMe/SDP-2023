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
     * Add a user to the database
     * @param user The user to add
     * @return A future that will complete when the user has been added
     */
    fun updateUser(user: UserInfo): CompletableFuture<Void>

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
    fun addEventsToUser(email: String, events: List<Event>): CompletableFuture<Void>

    /**
     * Get the current user's email
     * @return The current user's email
     */
    fun getCurrentEmail(): String

    /**
     * Set the current user's email
     * @param email The email to set
     */
    fun setCurrentEmail(email: String)

}