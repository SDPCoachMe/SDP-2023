package com.github.sdpcoachme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database

import com.google.firebase.ktx.Firebase
import java.util.concurrent.CompletableFuture


class MainActivity : ComponentActivity() {

    private val db: DatabaseReference = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FirebaseForm()
                    GreetingForm()
                }
            }
        }
    }


    @Composable
    fun FirebaseForm() {
        var phoneText by remember { mutableStateOf("") }
        var emailText by remember { mutableStateOf("") }
        Column(//modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(//modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(modifier = Modifier.testTag("getButton"),
                    onClick = {
                        val future = CompletableFuture<String>()

                        db.child(phoneText).get().addOnSuccessListener {
                            if (it.value == null) future.completeExceptionally(NoSuchFieldException())
                            else future.complete(it.value as String)
                        }.addOnFailureListener {
                            future.completeExceptionally(it)
                        }

                        future.thenAccept {
                            emailText = it
                        }
                    },)
                {
                    Text("get")
                }
                Button(modifier = Modifier.testTag("setButton"),
                    onClick = {
                        db.child(phoneText).setValue(emailText)
                    },)
                {
                    Text("set")
                }

            }
            TextField(
                modifier = Modifier.testTag("textfield"),
                value = phoneText,
                onValueChange = { phoneText = it },
                label = { Text("Phone") }
            )
            TextField(
                modifier = Modifier.testTag("textfield"),
                value = emailText,
                onValueChange = { emailText = it },
                label = { Text("Email") }
            )

        }

    }
}



@Composable
fun GreetingForm() {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        //modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            modifier = Modifier.testTag("textfield"),
            value = text,
            onValueChange = { text = it },
            label = { Text("Your name") }
        )
        Button(
            modifier = Modifier.testTag("button"),
            onClick = {
            val intent = Intent(context, GreetingActivity::class.java)
            intent.putExtra("name", text)
            context.startActivity(intent)
        })
        { Text("DISPLAY MESSAGE") }
    }
}