package com.github.sdpcoachme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.sdpcoachme.firebase.auth.FirebaseUIActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void greeting(View view){
        TextView textView = findViewById(R.id.mainName);
        String str = textView.getText().toString();
        Intent intent = new Intent(MainActivity.this, GreetingActivity.class);
        intent.putExtra("name", str);
        startActivity(intent);
    }

    public void signIn(View view){
        Intent intent = new Intent(MainActivity.this, FirebaseUIActivity.class);

        startActivity(intent);
    }
}