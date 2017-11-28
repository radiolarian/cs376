package com.example.noon.cs376;

/**
 * Created by estra on 10/30/2017.
 */

public class RelativeAudioParser {
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
