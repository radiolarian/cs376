package com.example.noon.cs376;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by noon on 10/30/17.
 */

public class MainService extends Service {
    /*do RMS calculations here and send them to the watch.*/
    public static final String VIBRATE = "/vibrate";
    private GoogleApiClient googleApiClient;


    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d("tag", "onConnected");
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addApi(Wearable.API)
                .build();

        googleApiClient.connect();
    }


    //idk what any of this chunk does i just need it so i dont get errors
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }


    public void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( googleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            googleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }
}
