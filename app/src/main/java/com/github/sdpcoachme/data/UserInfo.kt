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
    val ratings: Map<String, Int> = emptyMap(),
    val sports: List<Sports> = emptyList(),
    val chatContacts: List<String> = listOf(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", Address(), false, emptyMap(), emptyList(), emptyList())

    /**
     * Overloaded version of getPictureResource(String) that uses the email of this UserInfo
     */
    fun getPictureResource(): Int {
        return getPictureResource(email)
    }

    companion object {
        /**
         * Returns the resource id for the profile picture of the user with given email. Note that
         * this is temporary, and will be replaced by a real profile picture in a future version.
         * For now, this functions hashes the user's email and returns one of the predefined profile
         * pictures located in the drawable folder.
         *
         * @param email The email of the user whose profile picture to return
         */
        // TODO: this method is temporary
        fun getPictureResource(email: String): Int {
            return when (email) {
                "lindsayski@gmail.com" ->
                    R.drawable.lindsay
                "ennassih.yann@gmail.com" ->
                    R.drawable.yann
                "damian.kopp01@gmail.com" ->
                    R.drawable.damian
                "luca.aengu@gmail.com" ->
                    R.drawable.luca
                "gotti.bry@gmail.com" ->
                    R.drawable.bryan
                else ->
                    R.drawable::class.java.fields.firstOrNull {
                        it.name == email.split("@").first().replace(".", "_")
                    }?.get(null) as Int? ?: R.drawable.loading_picture
            }
            // Empty email means UserInfo is probably loading, so return gray background picture
//            if (email.isEmpty()) {
//                return R.drawable.loading_picture
//            }
//
//            val prefix = "profile_picture_"
//            val fieldNames = R.drawable::class.java.fields.filter {
//                it.name.startsWith(prefix)
//            }
//
//            // mod returns same sign as divisor, so no need to use abs
//            val index = email.hashCode().mod(fieldNames.size)
//            val field = fieldNames[index]
//
//            return field.get(null) as Int
        }
    }
}
