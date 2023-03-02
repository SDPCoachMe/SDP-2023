package com.github.sdpcoachme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.github.sdpcoachme.network.DataFormat
import com.github.sdpcoachme.repositories.DefaultRepositoryAccess
import com.github.sdpcoachme.repositories.Resource
import com.github.sdpcoachme.repositories.Status
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Response
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
open class BoredActivity : AppCompatActivity() {

    @Inject lateinit var repo : DefaultRepositoryAccess

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bored)


        //get all the elements of the interface
        val txt : TextView = findViewById(R.id.response)
        val requestButton = findViewById<Button>(R.id.request)
        val dbButton = findViewById<Button>(R.id.db)
        val dbDelete = findViewById<Button>(R.id.delete)

        dbButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val str = repo.getContentDB()
                txt.text = str
            }
        }

        dbDelete.setOnClickListener {
            repo.deleteContentDB()
            txt.text = "DB deleted"
        }

        requestButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val resp : Resource<Response<DataFormat>> = repo.apiCall()
                if (resp.status == Status.SUCCESS) {
                    txt.text = "Activity : " + (resp.data?.body()?.activity ?: "Null")
                    resp.data?.let { it1 -> repo.insertNewEntryDB(it1) }
                }else{
                    txt.text = resp.message
                    CoroutineScope(Dispatchers.IO).launch {
                        txt.text = repo.getRandomDB()
                    }
                }
            }
        }
    }
}