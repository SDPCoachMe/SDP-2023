package com.github.sdpcoachme.repositories

import com.github.sdpcoachme.network.DataFormat

import retrofit2.Response

interface RepositoryAccess {

    suspend fun getContentDB() : String

    suspend fun getRandomDB() : String

    fun deleteContentDB()

    fun insertNewEntryDB(response: Response<DataFormat>)

    suspend fun apiCall() : Resource<Response<DataFormat>>
}