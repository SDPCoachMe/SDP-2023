package com.github.sdpcoachme.firebase.database

import com.github.sdpcoachme.ListSport

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val location: String,
    val sports: List<ListSport>
) {
    companion object {

        /**
         * Convert a database response to a UserInfo object
         */
        fun userInfoFromDBResponse(it: Any?): UserInfo {
            val userMap = it as Map<*, *>
            val firstName = userMap["firstName"] as? String ?: ""
            val lastName = userMap["lastName"] as? String ?: ""
            val email = userMap["email"] as? String ?: ""
            val phone = userMap["phone"] as? String ?: ""
            val location = userMap["location"] as? String ?: ""
            val sportsList = userMap["sports"] as? List<*> ?: listOf("title" to "", "selected" to false)
            val sports = sportsList.map { sportMap ->
                val title = (sportMap as? Map<*, *> ?: mapOf<String, String>())["title"] as? String ?: ""
                val selected = (sportMap as? Map<*, *> ?: mapOf<String, Boolean>())["selected"] as? Boolean ?: false
                ListSport(title, selected)
            }
            return UserInfo(firstName, lastName, email, phone, location, sports)
        }
    }
}
