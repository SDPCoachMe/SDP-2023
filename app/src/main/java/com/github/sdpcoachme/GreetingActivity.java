package com.github.sdpcoachme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.sdpcoachme.firebase.auth.FirebaseUIActivity;

public class GreetingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greeting);

        TextView receiverMessage = findViewById(R.id.message);

        Intent intent = getIntent();
        String str = intent.getStringExtra("name");

        receiverMessage.setText(str);
    }
}