package com.github.sdpcoachme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.room.Room
import com.github.sdpcoachme.database.AppDB
import com.github.sdpcoachme.database.LineDB
import com.github.sdpcoachme.dependencyInjection.AppModule
import com.github.sdpcoachme.network.DataFormat
import com.github.sdpcoachme.network.RequestPoolAPI
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint

open class BoredActivity : AppCompatActivity() {

    @Inject lateinit var modules : AppModule
    @SuppressLint("MissingInflatedId", "SetTextI18n")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bored)

        val providedDB = modules.provideDatabase(applicationContext)
        val db = modules.provideDao(providedDB)
        val remoteDataSource = modules.provideApi()

        val txt : TextView = findViewById(R.id.response)
        val requestButton = findViewById<Button>(R.id.request)
        val dbButton = findViewById<Button>(R.id.db)
        val dbDelete = findViewById<Button>(R.id.delete)

        dbButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val dbTable : List<LineDB> = db.getAll()
                var str = ""
                for (line in dbTable){
                    str = str + line.activity + "\n"
                }
                txt.text = str
            }
        }

        dbDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                db.deleteDB()
                txt.text = "DB deleted"
            }
        }

        //onclickListener for the api request button
        requestButton.setOnClickListener {
            remoteDataSource.getActivity().enqueue(object : Callback<DataFormat> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<DataFormat>, response: Response<DataFormat>) {
                    if (response.code() != 200){
                        txt.text = "Error getting the activity"
                        return
                    }
                    txt.text = "Activity : " + (response.body()?.activity ?: "Null")
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
                @SuppressLint("SetTextI18n")
                override fun onFailure(call: Call<DataFormat>, t: Throwable) {
//                    txt.text = "Error no internet connection ..."
                    CoroutineScope(Dispatchers.IO).launch {
                        val entriesDB = db.getAll()
                        if (entriesDB.isNotEmpty()){
                            val rand = Random.nextInt(entriesDB.size)
                            val randElem = entriesDB[rand]
                            txt.text = "No internet connection : seeing cached data : " + randElem.activity
                        }else{
                            txt.text = "No internet connection : no cached data"
                        }
                    }
                }
            })
        }
    }
}