package com.example.noon.cs376;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import AlizeSpkRec.*;
import AlizeSpkRec.BuildConfig;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;


/**
 * Created by noon on 11/11/17.
 */

class AlizeSpeechRecognizer {

    private static final String SPEAKER_ID = "speaker";
    private int modelNumber = 0;
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
//        InputStream configAsset = context.getAssets().open("config.cfg");
        String config = "########################################################\n" +
                "#      Miscellaneous\n" +
                "########################################################\n" +
                "bigEndian                   false\n" +
                "featureServerMemAlloc       10000000\n" +
                "featureServerBufferSize     ALL_FEATURES\n" +
                "featureServerMode           FEATURE_WRITABLE\n" +
                "frameLength                 0.01\n" +
                "sampleRate                  44100\n" +
                "segmentalMode               false\n" +
                "debug                       false\n" +
                "verboseLevel                1\n" +
                "\n" +
                "\n" +
                "########################################################\n" +
                "#      Computation\n" +
                "########################################################\n" +
                "topDistribsCount            10\n" +
                "computeLLKWithTopDistribs   COMPLETE\n" +
                "maxLLK                      200\n" +
                "minLLK                      -200\n" +
                "channelCompensation         none\n" +
                "nbTrainIt                   1\n" +
                "MAPAlgo                     MAPOccDep\n" +
                "meanAdapt                   true\n" +
                "MAPRegFactorMean            14.0\n" +
                "regulationFactor            14.0\n" +
                "MAPAlpha                    0.5\n" +
                "#inputWorldFilename         world\n" +
                "\n" +
                "\n" +
                "########################################################\n" +
                "#      Formats and paths\n" +
                "########################################################\n" +
                "mixtureFilesPath            gmm/\n" +
                "loadMixtureFileFormat       RAW\n" +
                "loadMixtureFileExtension    .gmm\n" +
                "saveMixtureFileFormat       RAW\n" +
                "saveMixtureFileExtension    .gmm\n" +
                "\n" +
                "featureFilesPath            prm/\n" +
                "loadFeatureFileFormat       SPRO4\n" +
                "loadFeatureFileExtension    .prm\n" +
                "saveFeatureFileFormat       SPRO4\n" +
                "saveFeatureFileExtension    .prm\n" +
                "\n" +
                "audioFilesPath              audio/\n" +
                "\n" +
                "\n" +
                "########################################################\n" +
                "#      Feature options\n" +
                "########################################################\n" +
                "loadFeatureFileBigEndian    false\n" +
                "addDefaultLabel             false\n" +
                "defaultLabel                speech\n" +
                "labelSelectedFrames         speech\n" +
                "featureServerMask           0-18,20-50\n" +
                "vectSize                     50\n" +
                "\n" +
                "\n" +
                "########################################################\n" +
                "#      Parameterization options\n" +
                "########################################################\n" +
                "SPRO_sampleRate              44100\n" +
                "SPRO_f_max                   0\n" +
                "SPRO_f_min                   0\n" +
                "SPRO_emphco                  0.97\n" +
                "SPRO_nfilters                24\n" +
                "SPRO_numceps                 19\n" +
                "SPRO_lifter                  22\n" +
                "SPRO_usemel                  true\n" +
                "SPRO_format                  SPRO_SIG_PCM16_FORMAT\n" +
                //"SPRO_lswap                   true\n" +
                "SPRO_add_energy\n" +
                "SPRO_add_delta\n" +
                "SPRO_add_acceleration\n";
        InputStream stream = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8.name()));

        recognizer = new SimpleSpkDetSystem(stream, context.getFilesDir().getPath());
//        Log.d("alize", "path is ," + context.getFilesDir().getPath());
//        configAsset.close();

        InputStream backgroundModelAsset = context.getAssets().open("gmm/world.gmm");
        recognizer.loadBackgroundModel(backgroundModelAsset);
        backgroundModelAsset.close();

        System.out.println("!!!!!! System status:");
        System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
        System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
        System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
    }

    public void addNewAudioSample(byte[] audio)
    {
        // Send audio to the system
        try {
            recognizer.addAudio(audio);
            System.out.println("Adding new audio sample");    // true
        } catch (Exception e) {
            Log.e("Alize", e.toString());
        }
    }

    public void addNewAudioSample(short[] audio)
    {
        // Send audio to the system
        try {
            byte[] byteArray = new byte[audio.length << 1];
            for (int i = 0; i < audio.length; i++)
            {
                byteArray[i<<1] = (byte) ((audio[i] >> 8) & 0xff);
                byteArray[(i<<1) + 1] = (byte) (audio[i] & 0xff);
            }
            recognizer.addAudio(byteArray);
            /*
            System.out.print("New sample shorts, size=" + audio.length + ": ");
            for (short s : audio)
            {
                System.out.print(s + ", ");
            }
            System.out.println();

            System.out.print("New sample bytes, size=" + byteArray.length + ": ");
            for (byte s : byteArray)
            {
                System.out.print(s + ", ");
            }
            System.out.println();
            */
            System.out.println("Adding new audio sample");    // true
            System.out.println("System status after adding:");
            System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
            System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
            System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
        } catch (Exception e) {
            Log.e("Alize", e.toString());
        }
    }

    public void trainModel()
    {
        try {
            // Train a model with the audio
            recognizer.createSpeakerModel(SPEAKER_ID + modelNumber);

            System.out.println("System status after training:");
            System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
            System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
            System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true

            resetAll();

            modelNumber++;
        } catch (Exception e) {
            Log.e("Alize", e.toString() + "\n" + e.getStackTrace().toString());
        }
    }

    public void testModel()
    {
        try {
            // Perform speaker verification against the model we created earlier
            System.out.println("System status before testing:");
            System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
            System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
            System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
            SimpleSpkDetSystem.SpkRecResult verificationResult = recognizer.verifySpeaker(SPEAKER_ID);
            Log.d("Alize", "Test result: Match?: " + verificationResult.match + ", Score: " + verificationResult.score);
            resetAll();
        } catch (Exception e) {
            Log.e("Alize", e.toString());
        }
    }

    public void identifySpeaker()
    {
        try {
            // Perform speaker verification against the model we created earlier
            System.out.println("System status before testing:");
            System.out.println("  # of features: " + recognizer.featureCount());   // at this point, 0
            System.out.println("  # of models: " + recognizer.speakerCount());     // at this point, 0
            System.out.println("  UBM is loaded: " + recognizer.isUBMLoaded());    // true
            SimpleSpkDetSystem.SpkRecResult verificationResult = recognizer.identifySpeaker();
            Log.d("Alize", "Test result: Match?: " + verificationResult.match + ", Speaker: " + verificationResult.speakerId + ", Score: " + verificationResult.score);
            resetAll();
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
            result = recognizer.verifySpeaker(SPEAKER_ID);
        }  catch (Exception e) {
        Log.e("Alize", e.toString());
        }
        return result;
    }
}
