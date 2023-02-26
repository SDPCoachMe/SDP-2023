package com.example.bootcamp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

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