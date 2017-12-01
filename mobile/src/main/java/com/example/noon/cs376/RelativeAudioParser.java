package com.example.noon.cs376;

/**
 * Created by estra on 10/30/2017.
 */

public class RelativeAudioParser {
    private final static double FREQUENCY_DISTANCE_THRESHOLD = 3; // the allowable distance in hertz is this number * sample_rate / num_fft_bins

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

    public static double[] getCurrentBins()
    {
        return currentBins;
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
        int currentMode = 0;
        double maxBin = 0;
        for (int i = 0; i < subtractedBins.length; i++)
        {
            subtractedBins[i] -= envFrequency[i];
            if (subtractedBins[i] > maxBin)
            {
                maxBin = subtractedBins[i];
                currentMode = i;
            }
        }
        return (Math.abs(currentMode - speakerMode) <= FREQUENCY_DISTANCE_THRESHOLD);
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
