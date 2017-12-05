package com.example.noon.cs376;

import java.util.Arrays;

import static com.example.noon.cs376.DSP.normalize;
import static com.example.noon.cs376.DSP.printArray;

/**
 * Created by estra on 10/30/2017.
 */

public class RelativeAudioParser {
    private final static double FREQUENCY_DISTANCE_THRESHOLD = 3; // the allowable distance in hertz is this number * sample_rate / num_fft_bins
    private final static double MATCH_ALPHA = .7;

    static int speakerMode;
    static int numRelevantBins;
    static double[] speakerBins;
    static double[] currentBins;
    static boolean speakerFrequencySet = false;

    public static void addToBins(double[] f)
    {
        for (int i = 0; i < numRelevantBins; i++)
        {
            currentBins[i] += f[i];
        }
    }

    public static void subtractFromBins(double[] f)
    {
        for (int i = 0; i < numRelevantBins; i++)
        {
            currentBins[i] -= f[i];
        }
    }

    public static double[] getCurrentBins()
    {
        return currentBins;
    }

    public static double[] getSpeakerBins()
    {
        return speakerBins;
    }

    public static boolean isSpeakerFrequencySet()
    {
        return speakerFrequencySet;
    }

    public static void setBinsAsSpeaker()
    {
        speakerBins = currentBins;
        speakerMode = 0;
        double maxBin = 0;
        for (int i = 0; i < speakerBins.length; i++)
        {
            if (speakerBins[i] > maxBin)
            {
                maxBin = speakerBins[i];
                speakerMode = i;
            }
        }
        speakerFrequencySet = true;
    }

    public static void resetCurrentBins()
    {
        currentBins = new double[numRelevantBins];
    }

    public static boolean isSpeakerMatch(double[] envFrequency)
    {
        double[] subtractedBins = currentBins.clone();

        if (envFrequency != null)
        {
            for (int i = 0; i < subtractedBins.length; i++) {
                subtractedBins[i] -= envFrequency[i];
            }
        }

        double[] normCurrent = DSP.normalize(subtractedBins);
        double[] normSpeaker = DSP.normalize(speakerBins);

        double perfectCorr = DSP.max(DSP.sum(DSP.times(normSpeaker, normSpeaker)), DSP.sum(DSP.times(normCurrent, normCurrent)));

        double[] xcorr = DSP.xcorr(normCurrent, normSpeaker);

        double alpha = (DSP.max(xcorr) / perfectCorr);

        System.out.print("Xcorr coeff: " + alpha + " --- ");
        printArray(xcorr);

        return alpha >= MATCH_ALPHA;
    }

    public static boolean isSpeakerMatch()
    {
        return isSpeakerMatch(null);
    }

    public static int getSpeakerFrequency()
    {
        return (int)((float) speakerMode * MainActivity.SAMPLE_RATE / (numRelevantBins << 1));
    }

    public static int getCurrentFrequency()
    {
        int currentMode = 0;
        double maxBin = 0;
        for (int i = 0; i < currentBins.length; i++)
        {
            if (currentBins[i] > maxBin)
            {
                maxBin = currentBins[i];
                currentMode = i;
            }
        }
        return currentMode * MainActivity.SAMPLE_RATE / (numRelevantBins << 1);
    }

    public static void Init(int bufferSize)
    {
        numRelevantBins = bufferSize >> 1;
        speakerBins = new double[numRelevantBins];
        currentBins = new double[numRelevantBins];
    }

    public RelativeAudioParser(int bufferSize)
    {
        numRelevantBins = bufferSize >> 1;
        speakerBins = new double[numRelevantBins];
        currentBins = new double[numRelevantBins];
    }

    public static void logCurrentBins()
    {
        for (double d : currentBins)
        {
            System.out.println(d);
        }
        System.out.println();
    }

    // Returns the RMS value as a float for an array of values
    public static float RMS(short[] buffer)
    {
        float sum = 0f;
        for (int i = 0; i < buffer.length; i++)
        {
            sum += buffer[i] * buffer[i];
        }
        return (float) Math.sqrt(sum / buffer.length);
    }
}
