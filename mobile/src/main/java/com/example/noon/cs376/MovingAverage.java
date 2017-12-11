package com.example.noon.cs376;

/**
 * Created by noon on 11/28/17.
 */
/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

        import java.util.LinkedList;
        import java.util.Queue;

/**
 * Helper to calculate the moving average of an array.
 */

public class MovingAverage {
    private static final Float tolerance = 150f;
    private Queue<Float> mWindow = new LinkedList<Float>();
    private Queue<Float> mCandidateWindow = new LinkedList<Float>();
    private Queue<double[]> mFftWindow = new LinkedList<double[]>();
    private Queue<double[]> mCandidateFftWindow = new LinkedList<double[]>();
    private int mPeriod;
    private Float mSum = 0f;
    private Float mCandidateSum = 0f;
    private double[] mFftBins;
    private double[] mCandidateFftBins;
    private int mNumFftBins;

    public MovingAverage(int period, int fftBins) {
        mPeriod = period;
        mNumFftBins = fftBins;
        mFftBins = new double[fftBins];
        mCandidateFftBins = new double[fftBins];
    }

    // Moving average has more complex logic under the hood
    // When new values are added, if they are compatible with our moving average, we adjust it.
    // Otherwise, we begin trying to fill a new moving average, and when complete with enough consecutive samples, we swap it as active
    public void add(Float value, double[] fftMag) {
        if (Math.abs(value - getAverage()) <= tolerance)
        {
            mSum = mSum + value;
            mWindow.add(value);
            mFftWindow.add(fftMag);
            addFftBins(fftMag);
            if (mWindow.size() > mPeriod) {
                mSum = mSum - mWindow.remove();
                subtractFftBins(mFftWindow.remove());
            }
        }
        else
        {
            if (Math.abs(value - getCandidateAverage()) <= tolerance) {
                mCandidateSum = mCandidateSum + value;
                mCandidateWindow.add(value);
                mCandidateFftWindow.add(fftMag);
                addCandidateFftBins(fftMag);
                if (mCandidateWindow.size() == mPeriod) {
                    System.out.println("Swapping!");
                    mWindow = mCandidateWindow;
                    mFftWindow = mCandidateFftWindow;
                    mSum = mCandidateSum;
                    mFftBins = mCandidateFftBins;
                    clearCandidate();
                }
            }
            else
            {
                clearCandidate();
            }
        }
    }

    public Float getAverage() {
        if (mWindow.isEmpty()) {
            // Use negative 1 for undefined in this case
            return -1f;
        }
        return mSum / mWindow.size();
    }

    public int getFrequency()
    {
        int currentMode = 0;
        double maxBin = 0;
        for (int i = 0; i < mFftBins.length; i++)
        {
            if (mFftBins[i] > maxBin)
            {
                maxBin = mFftBins[i];
                currentMode = i;
            }
        }
        return currentMode * MainActivity.SAMPLE_RATE / (mFftBins.length << 1);
    }

    public Float getCandidateAverage() {
        if (mCandidateWindow.isEmpty()) {
            // Use negative 1 for undefined in this case
            return -1f;
        }
        return mCandidateSum / mCandidateWindow.size();
    }

    public void clear() {
        mWindow.clear();
        mFftWindow.clear();
        mSum = 0f;
        mFftBins = new double[mNumFftBins];
        clearCandidate();
    }

    public void clearCandidate()
    {
        mCandidateWindow.clear();
        mCandidateFftWindow.clear();
        mCandidateSum = 0f;
        mCandidateFftBins = new double[mNumFftBins];
    }

    public double[] getNormalizedFftBins()
    {
        double[] normalizedBins = new double[mNumFftBins];
        for (int i = 0; i < mFftBins.length; i++)
        {
            normalizedBins[i] = mFftBins[i] / mPeriod;
        }
        return normalizedBins;
    }

    private void addFftBins(double[] newFft)
    {
        for (int i = 0; i < newFft.length; i++)
        {
            mFftBins[i] += newFft[i];
        }
    }

    private void subtractFftBins(double[] newFft)
    {
        for (int i = 0; i < newFft.length; i++)
        {
            mFftBins[i] -= newFft[i];
        }
    }

    private void addCandidateFftBins(double[] newFft)
    {
        for (int i = 0; i < newFft.length; i++)
        {
            mCandidateFftBins[i] += newFft[i];
        }
    }

    private void subtractCandidateFftBins(double[] newFft)
    {
        for (int i = 0; i < newFft.length; i++)
        {
            mCandidateFftBins[i] -= newFft[i];
        }
    }
}