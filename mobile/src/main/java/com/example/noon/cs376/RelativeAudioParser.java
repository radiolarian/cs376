package com.example.noon.cs376;

/**
 * Created by estra on 10/30/2017.
 */

public class RelativeAudioParser {
    private final static double FREQUENCY_DISTANCE_THRESHOLD = 2; // the allowable distance in hertz is this number * sample_rate / num_fft_bins

    int speakerMode;
    int numRelevantBins;
    double[] speakerBins;
    double[] currentBins;

    public void addToBins(double[] f)
    {
        for (int i = 0; i < numRelevantBins; i++)
        {
            currentBins[i] += f[i];
        }
    }

    public void setBinsAsSpeaker()
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
    }

    public void resetCurrentBins()
    {
        currentBins = new double[numRelevantBins];
    }

    public boolean isSpeakerMatch()
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
        return (Math.abs(currentMode - speakerMode) <= FREQUENCY_DISTANCE_THRESHOLD);
    }

    public int getSpeakerFrequency()
    {
        return speakerMode * MainActivity.SAMPLE_RATE / (numRelevantBins << 1);
    }

    public int getCurrentFrequency()
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

    public RelativeAudioParser(int bufferSize)
    {
        numRelevantBins = bufferSize >> 1;
        speakerBins = new double[numRelevantBins];
        currentBins = new double[numRelevantBins];
    }

    // Returns the RMS value (in String format) for an array of values
    public static String RMS(short[] buffer)
    {
        double sum = 0.0;
        for (int i = 0; i < buffer.length; i++)
        {
            sum += buffer[i] * buffer[i];
        }
        return Double.toString(Math.sqrt(sum / buffer.length));
    }
}
