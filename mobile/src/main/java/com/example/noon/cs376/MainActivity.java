package com.example.noon.cs376;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start sensing service
        Intent intent = new Intent(this, MainService.class);
        startService(intent);

        //connect to watch


    }
}
