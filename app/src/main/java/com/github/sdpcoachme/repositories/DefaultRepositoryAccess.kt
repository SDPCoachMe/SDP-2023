package com.github.sdpcoachme.repositories

import com.github.sdpcoachme.database.LineDB
import com.github.sdpcoachme.database.RequestPoolDB
import com.github.sdpcoachme.network.DataFormat
import com.github.sdpcoachme.network.RequestPoolAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import javax.inject.Inject
import kotlin.random.Random

class DefaultRepositoryAccess @Inject constructor(
        private val db : RequestPoolDB,
        private val api : RequestPoolAPI
) : RepositoryAccess{

    override suspend fun getContentDB(): String = withContext(Dispatchers.IO) {
        val dbTable : List<LineDB> = db.getAll()
        var str = ""
        for (line in dbTable){
            str = str + line.activity + "\n"
        }
        str
    }

    override suspend fun getRandomDB(): String = withContext(Dispatchers.IO) {
        val str: String
        val entriesDB = db.getAll()
        str = if (entriesDB.isNotEmpty()){
            val rand = Random.nextInt(entriesDB.size)
            val randElem = entriesDB[rand]
            "No internet connection : seeing cached data : " + randElem.activity
        }else{
            "No internet connection : no cached data"
        }
        str
    }

    override fun deleteContentDB() {
        CoroutineScope(Dispatchers.IO).launch {
            db.deleteDB()
        }
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
        CoroutineScope(Dispatchers.IO).launch {
            db.insert(newEntry)
        }
    }

    override fun apiCall() : Call<DataFormat> {
        return api.getActivity()
    }


}