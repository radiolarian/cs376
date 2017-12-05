package com.example.noon.cs376;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

public class WatchActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);

        mTextView = findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
        Log.d("watch", "watch oncreate called");
    }
}
