package com.github.sdpcoachme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GreetingActivity extends AppCompatActivity implements View.OnClickListener {
    Button mapButton;
    Intent mapIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greeting);

        TextView receiverMessage = findViewById(R.id.message);

        Intent intent = getIntent();
        String str = intent.getStringExtra("name");

        receiverMessage.setText(str);
        mapButton = findViewById(R.id.open_maps_btn);
        mapButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        /*if (v.getId() == R.id.open_maps_btn) {
            getSupportFragmentManager().beginTransaction().replace(R.id.open_maps_btn, new MapsFragment()).commit();
        }*/
        //Uri gmIntentUri = Uri.parse("geo:46.520536,6.568318");
        //mapIntent = new Intent(Intent.ACTION_VIEW, gmIntentUri);
        mapIntent = new Intent(GreetingActivity.this, MapsActivity.class);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }

    }
}