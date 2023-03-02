package com.github.sdpcoachme.repositories

import com.github.sdpcoachme.database.LineDB
import com.github.sdpcoachme.network.DataFormat
import com.github.sdpcoachme.utility.ConstantsTest.DEFAULT_OK_DOUBLE
import com.github.sdpcoachme.utility.ConstantsTest.DEFAULT_OK_INT
import com.github.sdpcoachme.utility.ConstantsTest.DEFAULT_OK_STRING
import retrofit2.Response

class FakeRepositoryAccess : RepositoryAccess{

    private val dbItems = mutableListOf<LineDB>()
    override suspend fun getContentDB(): String {
        return DEFAULT_OK_STRING
    }

    override suspend fun getRandomDB(): String {
        return DEFAULT_OK_STRING
    }

    override fun deleteContentDB() {
        dbItems.clear()
    }

    override fun insertNewEntryDB(response: Response<DataFormat>) {
        val resp = response.body()
        val newEntry = LineDB(System.currentTimeMillis().hashCode(),
                resp?.activity,
                resp?.type,
                resp?.participants,
                resp?.price,
                resp?.link,
                resp?.key,
                resp?.accessibility)
        dbItems.add(newEntry)
    }

    override suspend fun apiCall(): Resource<Response<DataFormat>> {
        return Resource.success(Response.success(DataFormat(
                DEFAULT_OK_STRING,
                DEFAULT_OK_STRING,
                DEFAULT_OK_INT,
                DEFAULT_OK_DOUBLE,
                DEFAULT_OK_STRING,
                DEFAULT_OK_INT,
                DEFAULT_OK_DOUBLE
        )))
    }
}