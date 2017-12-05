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
    private static final String WOZPATH = "/woz";

    //    private static final double VIBRATION_THRESHOLD = 5000.0;
//    private static final double VIBRATION_THRESHOLD_LOUD = 15000.0;
//    private static final long[] vibrationPattern = {0, 250}; //wait time, on time
    private static final long[] vibrationPatternLoud = {0, 100, 100, 100, 250, 100, 100, 100}; //wait time, on time - vibes 4 times
    private static final long[] vibrationPatternSoft = {0, 100, 100, 100}; //wait time, on time - vibes 2 times

    final int indexInPatternToRepeat = -1;


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String s = new String(messageEvent.getData());
        Log.d("tag", s);
        Log.d("path", messageEvent.getPath());

        if( messageEvent.getPath().equalsIgnoreCase( WOZPATH ) ) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (s.equals("loud")) {
                vibrator.vibrate(vibrationPatternLoud, indexInPatternToRepeat);
                Log.d("tag", "Vibrated Loud");
            }
            else if (s.equals("soft")) {
                vibrator.vibrate(vibrationPatternSoft, indexInPatternToRepeat);
                Log.d("tag", "Vibrated Soft");
            }

        } else if (messageEvent.getPath().equalsIgnoreCase( PATH ) ) {
            //just vibrate like normal
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(vibrationPatternLoud, indexInPatternToRepeat);
            Log.d("tag", "Vibrated no WoZ");
        }
        else {
            super.onMessageReceived( messageEvent );
            Log.d("tag", "Other stuff happened");

        }
    }
}
