package com.example.bucephalus;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.TextView;

public class CANDebugActivity extends AppCompatActivity {

    String btsocket_string;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c_a_n_debug);

        Intent i = getIntent();
        btsocket_string = i.getStringExtra("btsocketinfo");

//        BluetoothSocket Btsocket = (BluetoothSocket)btsocket_string;

        TextView txt = findViewById(R.id.speed_info);

        txt.setText(btsocket_string);

    }
}
