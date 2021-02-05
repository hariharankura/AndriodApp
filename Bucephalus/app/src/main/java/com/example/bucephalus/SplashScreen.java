package com.example.bucephalus;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

public class SplashScreen extends AppCompatActivity {

    Boolean splashed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Thread splash = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    sleep(3200);
                } catch (Exception e) {

                }finally {
                    Intent i = new Intent(SplashScreen.this, MapsActivity.class);
//                    Intent i = new Intent(SplashScreen.this, MenuActivity.class);
                    startActivity(i);
                }
            }
        };

        splash.start();
    }

}
