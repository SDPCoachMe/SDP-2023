package com.example.CoachMe

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BoredActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bored)

        val txt : TextView = findViewById(R.id.response)

        val retrofit = Retrofit.Builder()
                .baseUrl("https://www.boredapi.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val boredApi = retrofit.create(BoredAPI::class.java)

        val requestButton = findViewById<Button>(R.id.request)

        requestButton.setOnClickListener {
            boredApi.getActivity().enqueue(object : Callback<DataFormat> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<DataFormat>, response: Response<DataFormat>) {
                    if (response.code() != 200){
                        txt.text = "Error getting the activity"
                        return
                    }
                    txt.text = "Activity : " + (response.body()?.activity ?: "Null")
                }
                @SuppressLint("SetTextI18n")
                override fun onFailure(call: Call<DataFormat>, t: Throwable) {
                    txt.text = "Error no internet connection ..."
                }
            })
        }
    }
}