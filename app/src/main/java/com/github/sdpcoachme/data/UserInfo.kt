package com.github.sdpcoachme.data

import com.github.sdpcoachme.R

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: Address,
    val coach: Boolean,
    val sports: List<Sports> = emptyList(),
    val chatContacts: List<String> = listOf(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", Address(), false, emptyList(), emptyList())

    /**
     * Overloaded version of getProfilePictureResource(String) that uses the email of the UserInfo
     */
    fun getProfilePictureResource(): Int {
        return getProfilePictureResource(email)
    }

    companion object {
        /**
         * Returns the resource id for the profile picture of the user with given email. Note that
         * this is temporary, and will be replaced by a real profile picture in a future version.
         * For now, this functions hashes the user's email and returns one of the predefined profile
         * pictures located in the drawable folder.
         */
        // TODO: this method is temporary
        fun getProfilePictureResource(email: String): Int {
            val prefix = "profile_picture_"
            val fieldNames = R.drawable::class.java.fields.filter {
                it.name.startsWith(prefix)
            }
            // mod returns same sign as divisor, so no need to use abs
            val index = email.hashCode().mod(fieldNames.size)
            val field = fieldNames[index]

            return field.get(null) as Int
        }
    }
}
