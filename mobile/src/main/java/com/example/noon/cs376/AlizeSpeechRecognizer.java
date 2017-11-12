package com.example.noon.cs376;

import java.io.IOException;
import java.io.InputStream;

import AlizeSpkRec.*;
import android.content.Context;
import android.util.Log;


/**
 * Created by noon on 11/11/17.
 */

class AlizeSpeechRecognizer {

    SimpleSpkDetSystem recognizer;

    public AlizeSpeechRecognizer(Context context)
    {
        try {
            initialize(context);
        } catch (Exception e)
        {
            Log.e("Alize", e.toString());

        }
    }

    private void initialize (Context context) throws IOException, AlizeException {
        InputStream configAsset = context.getAssets().open("AlizeDefault.cfg");
        recognizer = new SimpleSpkDetSystem(configAsset, context.getFilesDir().getPath());
        configAsset.close();
        InputStream backgroundModelAsset = context.getAssets().open("gmm/world.gmm");
        recognizer.loadBackgroundModel(backgroundModelAsset);
        backgroundModelAsset.close();

        System.out.println("System status:");
        System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
        System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
        System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
    }

    public void trainSpeakerModel(byte[] audio)
    {
        // Send audio to the system
        try {
            recognizer.addAudio(audio);

            // Train a model with the audio
            recognizer.createSpeakerModel("Somebody");

            System.out.println("System status after training:");
            System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
            System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
            System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
        } catch (Exception e) {
            Log.e("Alize", e.toString());

        }
    }

    public void resetAudio()
    {
        try {
            recognizer.resetAudio();
        } catch (Exception e) {
            Log.e("Alize", e.toString());

        }
    }

    public void resetAll()
    {
        try {
            recognizer.resetAudio();
            recognizer.resetFeatures();
        } catch (Exception e) {
            Log.e("Alize", e.toString());

        }
    }

    public SimpleSpkDetSystem.SpkRecResult verifySpeaker(byte[] audio)
    {
        SimpleSpkDetSystem.SpkRecResult result = new SimpleSpkDetSystem.SpkRecResult();
        try {
            // Send the new audio to the system
            recognizer.addAudio(audio);

            // Perform speaker verification against the model we created earlier
            result = recognizer.verifySpeaker("Somebody");
        }  catch (Exception e) {
        Log.e("Alize", e.toString());
        }
        return result;
    }
}
