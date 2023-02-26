package com.github.sdpcoachme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.room.Room
import com.github.sdpcoachme.database.AppDB
import com.github.sdpcoachme.database.LineDB
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.*
import kotlin.random.Random


class BoredActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bored)

        val txt : TextView = findViewById(R.id.response)

        //Create a retrofit object that will handle the connection to the api and covert them into JSon
        val retrofit = Retrofit.Builder()
                .baseUrl("https://www.boredapi.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        //create an object that can handle the request fromm the pool of commands
        val requestPoolApi = retrofit.create(RequestPoolAPI::class.java)

        val requestButton = findViewById<Button>(R.id.request)
        val dbButton = findViewById<Button>(R.id.db)
        val dbDelete = findViewById<Button>(R.id.delete)

        // defines a db entity
        val db = Room.databaseBuilder(
                applicationContext,
                AppDB::class.java, "AppDB"
        ).build()

        val userDB = db.userDB() //creates an instance of the db

        dbButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val dbTable : List<LineDB> = userDB.getAll()
                var str = ""
                for (line in dbTable){
                    str = str + line.activity + "\n"
                }
                txt.text = str
            }
        }

        dbDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                userDB.deleteDB()
                txt.text = "DB deleted"
            }
        }

        //onclickListener for the api request button
        requestButton.setOnClickListener {
            requestPoolApi.getActivity().enqueue(object : Callback<DataFormat> {
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
                        userDB.insert(newEntry)
                    }
                }
                @SuppressLint("SetTextI18n")
                override fun onFailure(call: Call<DataFormat>, t: Throwable) {
//                    txt.text = "Error no internet connection ..."
                    CoroutineScope(Dispatchers.IO).launch {
                        val entriesDB = userDB.getAll()
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