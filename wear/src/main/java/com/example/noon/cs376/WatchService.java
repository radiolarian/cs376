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
    private static final String VIBRATE = "/vibrate";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("tag", "Watch Listener activated ");

        if( messageEvent.getPath().equalsIgnoreCase( VIBRATE ) ) {
            /*do some vibrations*/
            //TODO if RMS > _: ...
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 500, 50, 300}; //in ms i think...i copied this from SoF
            //-1 - don't repeat
            final int indexInPatternToRepeat = -1;
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            Log.d("tag", "Vibrated");


        } else {
            super.onMessageReceived( messageEvent );
            Log.d("tag", "Other stuff happened");

        }
    }
}
