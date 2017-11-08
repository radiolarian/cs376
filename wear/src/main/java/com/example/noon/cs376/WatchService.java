package com.example.noon.cs376;


import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

/**
 * Created by noon on 10/30/17.
 */

public class WatchService extends WearableListenerService {
    /*variables for path control*/
    private static final String PATH = "/watch";
    private static final double VIBRATION_THRESHOLD = 1000.0;
    private static final double VIBRATION_THRESHOLD_LOUD = 2000.0;
    private static final long[] vibrationPattern = {0, 250}; //wait time, on time
    private static final long[] vibrationPatternLoud = {0, 250, 100, 500}; //wait time, on time
    final int indexInPatternToRepeat = -1;


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String s = new String(messageEvent.getData());
        Log.d("tag", s);

        if( messageEvent.getPath().equalsIgnoreCase( PATH ) ) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (Double.parseDouble(s) >= VIBRATION_THRESHOLD_LOUD) {
                vibrator.vibrate(vibrationPatternLoud, indexInPatternToRepeat);
                Log.d("tag", "Vibrated Loudly");
            }
            else if (Double.parseDouble(s) >= VIBRATION_THRESHOLD) {
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                Log.d("tag", "Vibrated");
            }

        } else {
            super.onMessageReceived( messageEvent );
            Log.d("tag", "Other stuff happened");

        }
    }
}
