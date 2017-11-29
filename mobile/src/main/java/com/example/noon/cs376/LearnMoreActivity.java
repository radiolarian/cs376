package com.example.noon.cs376;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;




public class LearnMoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn_more);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        final Button mythButton = findViewById(R.id.myth_button);
        final Button guideButton = findViewById(R.id.guide_button);
        final Button hearingTestButton = findViewById(R.id.hearing_test_button);

        mythButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){

                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.betterhearing.org/hearingpedia/myths-about-hearing-loss")));

                    return true;
                }
                return false;
            }
        });

        guideButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){

                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.healthyhearing.com/help/hearing-loss")));

                    return true;
                }
                return false;
            }
        });

        hearingTestButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){

                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hear-it.org/hearing-test")));

                    return true;
                }
                return false;
            }
        });

    }

}
