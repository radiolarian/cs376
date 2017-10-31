package com.example.noon.cs376;

/**
 * Created by estra on 10/30/2017.
 */

public class RelativeAudioParser {
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
