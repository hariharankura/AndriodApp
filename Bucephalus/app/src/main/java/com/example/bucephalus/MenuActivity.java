package com.example.bucephalus;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    Button btconnect_button;
    Button debug_button;
    Button setdestination_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        btconnect_button = findViewById(R.id.btconnect_button);
        debug_button = findViewById(R.id.DebugButton);
        setdestination_button = findViewById(R.id.map_button);

        btconnect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, BtActivity.class);
                startActivity(i);
            }
        });

        debug_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, CANDebugActivity.class);
                startActivity(i);
            }
        });

        setdestination_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });

    }

}
