package com.github.sdpcoachme.firebase.database

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.junit.Test

class UserInfoTest {

    @Test
    fun userInfoCorrectlyParsesDatabaseResponse() {
        //user info correctly parses database response
        val firstName = "Some"
        val surname = "Person"
        val tennis = "Tennis"
        val isTennisFavorite = true
        val swimming = "Swimming"
        val isSwimmingFavorite = false
        val phone = "0000000000"
        val location = "123456789"
        val email = "some@email.com"

        val dbResponse = mapOf(
            "firstName" to firstName,
            "lastName" to surname,
            "sports" to listOf(
                mapOf("title" to tennis, "selected" to isTennisFavorite),
                mapOf("title" to swimming, "selected" to isSwimmingFavorite)
            ),
            "phone" to phone,
            "location" to location,
            "email" to email
        )

        val userInfo = UserInfo.userInfoFromDBResponse(dbResponse)

        assertThat(userInfo.firstName, `is`(firstName))
        assertThat(userInfo.lastName, `is`(surname))
        assertThat(userInfo.email, `is`(email))
        assertThat(userInfo.phone, `is`(phone))
        assertThat(userInfo.location, `is`(location))
        assertThat(userInfo.sports.size, `is`(2))
        assertThat(userInfo.sports[0].title, `is`(tennis))
        assertThat(userInfo.sports[0].selected, `is`(isTennisFavorite))
        assertThat(userInfo.sports[1].title, `is`(swimming))
        assertThat(userInfo.sports[1].selected, `is`(isSwimmingFavorite))
    }

    @Test
    fun userInfoHandlesMissingValues() {
        val dbResponse = mapOf<String, Any>()

        val userInfo = UserInfo.userInfoFromDBResponse(dbResponse)

        assertThat(userInfo.firstName, `is`(""))
        assertThat(userInfo.lastName, `is`(""))
        assertThat(userInfo.email, `is`(""))
        assertThat(userInfo.phone, `is`(""))
        assertThat(userInfo.location, `is`(""))
        assertThat(userInfo.sports.size, `is`(2))
        assertThat(userInfo.sports[0].title, `is`(""))
        assertThat(userInfo.sports[0].selected, `is`(false))
    }
}