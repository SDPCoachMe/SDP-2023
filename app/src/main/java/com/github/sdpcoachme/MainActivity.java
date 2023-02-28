package com.github.sdpcoachme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;

    Button setButton, getButton;
    EditText emailText, phoneText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void greeting(View view){
        button = findViewById(R.id.mainButton);
        textView = findViewById(R.id.mainName);
        String str = textView.getText().toString();
        Intent intent = new Intent(MainActivity.this, GreetingActivity.class);
        intent.putExtra("name", str);
        startActivity(intent);
    }


    public void get(View view) {
        getButton = findViewById(R.id.getButton);
        phoneText = findViewById(R.id.phoneText);
        emailText = findViewById(R.id.emailText);
        String phone = phoneText.getText().toString();
        String email = emailText.getText().toString();

        textView = findViewById(R.id.mainName);
        String str = textView.getText().toString();
        Intent intent = new Intent(MainActivity.this, GreetingActivity.class);
        intent.putExtra("name", str);
        startActivity(intent);
    }

    public void set(View view) {
        setButton = findViewById(R.id.setButton);
        phoneText = findViewById(R.id.phoneText);
        emailText = findViewById(R.id.emailText);
        String phone = phoneText.getText().toString();
        String email = emailText.getText().toString();
    }





}