package com.example.noon.cs376;

import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class TrainingActivity extends AppCompatActivity {

    private static final int MOVING_AVG_WINDOW_SIZE = 5; //num of audio samples to determine BG vol
    private static final int FREQUENCY = 8000;
    private static final int VIBRATION_DURATION = 400;
    private static final int DELAY_SAMPLES_AFTER_VIBRATION = 3;
    // Audio recording + play back

    // Dialogs
    ProgressDialog progressDialog;
    ProgressDialog mProgressDialog;
    ProgressDialog dProgressDialog;
    public static final int MFCC_DIALOG = 0;
    public static final int DRZ_DIALOG = 1;

    // File Locations
    public static final String AUDIO_FILE = "/sdcard/recordoutput.raw";
    public static final String CONFIG_FILE = "/sdcard/config.xml";
    public static final String MFCC_FILE = "/sdcard/test.mfc";
    public static final String UEM_FILE = "/sdcard/test.uem.seg";
    public static final String UBM_FILE = "/sdcard/test.ubm.gmm";
    public static final String IDENT_FILE = "/sdcard/test.ident.seg";

    // Audio Settings
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    public static final int SAMPLE_RATE = 44100;
    public static final int CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNELS_OUT = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
    public static final int PLAYBACK_MODE = AudioTrack.MODE_STREAM;

    //private static final double QUIET_THRESHOLD = 32768.0 * 0.02; //anything higher than 0.02% is considered non-silence
    private static final int MIN_BUFFER_SIZE_MULTIPLIER = 1;
    private int BUFFER_SIZE;
    private int RMS_WINDOW_SIZE;
    private AudioRecord _audioRecord;
    private AsyncTask<Void, Void, ParseResult> _task;
    private boolean bound = false;
    private boolean inNewSampleRecordingState = false;
    private float envNoiseLevel; //from movin avg
    private float LOUD_RATIO_THRESHOLD = 2.2f; //vibrate watch if x times softer/louder than env noise
    private float LOUD_MINIMUM_TRESHOLD = 800f;
    private boolean USE_WATCH_VIBRATION = false;
    private int samplesToDelay = 0;
    private boolean envNoiseSet = false;

    MainDatabase db;
    MainDao dao;

    MovingAverage movingavg;
    //RelativeAudioParser parser;
    FFT fft;
    private static final int FFT_BINS = 2048;
    int fftLoopsPerBuffer;

    //graph stuff
    LineGraphSeries<DataPoint> envNoise = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> speakerVol = new LineGraphSeries<>();


    //welcome message display stuff
    private int timesTriggered = 0;
    private int quiet_incidents = 0;
    private int moderate_incidents = 0;
    private int loud_incidents = 0;
    private static final float MODERATE_THRES = 5000;
    private static final float LOUD_THRES = 10000;

    //AlizeSpeechRecognizer alize;

    MainService mService;
    Button recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        Log.d("train", "created");
        //link the buttons
        recordButton = findViewById(R.id.record);
        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Button is not active until environmental level is established
                if (!envNoiseSet) { return false; };

                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Context context = getApplicationContext();
                    CharSequence text = "Now recording";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();

                    //new recordConvo().execute();

                    inNewSampleRecordingState = true;
                    // Do what you want
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    /*if (recorderWrapper != null) {
                        recorderWrapper.stop();
                    }*/
                    inNewSampleRecordingState = false;

                    Context context = getApplicationContext();
                    CharSequence text = "Finished recording";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();

                    // Train
                    Log.d("Train", "Starting training");
                    RelativeAudioParser.setBinsAsSpeaker();
                    RelativeAudioParser.resetCurrentBins();
                    Log.d("Train", "Speaker frequency is: " + RelativeAudioParser.getSpeakerFrequency() + " Hz");

                    /*
                    System.out.print("Speaker bins are: ");
                    double[] speakerBins = RelativeAudioParser.getSpeakerBins();
                    for (double d : speakerBins)
                    {
                        System.out.print(d + " ");
                    }
                    System.out.println();
                    */
                    //new trainTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    //Advance to History Screen
                    //startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    finish();

                    return true;
                }

                return false;
            }
        });

        // Set up audio buffer
        try
        {
            Log.d("Init", "Setting up AudioRecord");
            int tempBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS_OUT, AUDIO_FORMAT) * 8 * MIN_BUFFER_SIZE_MULTIPLIER;
            BUFFER_SIZE = 1 << ((int)(Math.log(tempBufferSize) / Math.log(2)) + 1); // ensure that buffer size is a power of two
            _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    CHANNELS_OUT, AUDIO_FORMAT, BUFFER_SIZE);
            Log.d("Init", "AudioRecord set up successfully");
            Log.d("Init", "Buffer size is: " + BUFFER_SIZE);

            // For now, let's set window size to buffer size (probably will need to adjust)
            RMS_WINDOW_SIZE = BUFFER_SIZE;
        }
        catch (Exception ex)
        {
            Log.d("Exception", ex.toString());
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //screen needs to be on for it to collect data

        // initialize and precompute FFT
        fft = new FFT(FFT_BINS);
        fftLoopsPerBuffer = BUFFER_SIZE / FFT_BINS;

        //parser = new RelativeAudioParser(FFT_BINS);
        //create a moving avg filter
        movingavg = new MovingAverage(MOVING_AVG_WINDOW_SIZE, FFT_BINS);


    }



    @Override
    protected void onStart() {
        super.onStart();
        // Create and bind service
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        bound = false;
        movingavg.clear();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        _audioRecord.startRecording();

        _task = new MonitorAudioTask();
        _task.execute(null, null, null);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        _task.cancel(true);
        _audioRecord.stop();
        movingavg.clear();

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    public void triggerVibrate()
    {
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Vibrate for 400 milliseconds
        v.vibrate(VIBRATION_DURATION);
    }

    private class MonitorAudioTask extends AsyncTask<Void, Void, ParseResult>
    {
        @Override
        protected ParseResult doInBackground(Void... params)
        {
            short[] buffer = new short[BUFFER_SIZE];
            int bufferReadResult = 0;
            ParseResult result = new ParseResult(ParseResult.ParseErrorCodes.CANCELLED, -1, -1, false);

            while (true)
            {
                if (isCancelled())
                    break;

                bufferReadResult = _audioRecord.read(buffer, 0, BUFFER_SIZE);
                short[] trimmedBuffer = Arrays.copyOfRange(buffer, 0, bufferReadResult);
                if (bufferReadResult > 0)
                {
                    double[] x, y;
                    double[] window = fft.getWindow();
                    int loops = (fftLoopsPerBuffer * 2) - 1;
                    int chunkSize = FFT_BINS >> 1;
                    for (int index = 0; index < loops; index++) {
                        x = new double[FFT_BINS];
                        y = new double[FFT_BINS];
                        for (int i = 0; i < chunkSize; i++) {
                            x[i] = window[i] * (double) buffer[(index * chunkSize) + i];
                            x[i + chunkSize] = window[i + chunkSize] * (double) buffer[(index * chunkSize) + i + chunkSize];
                        }
                        fft.fft(x, y);
                        RelativeAudioParser.addToBins(FFT.computeMagnitude(x, y));
                    }
                    //alize.addNewAudioSample(trimmedBuffer);

                    //byte[] audioBytes = new byte[2 * BUFFER_SIZE];
                    //ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buffer);
                    //alize.addNewAudioSample(audioBytes);

                    //alize.resetAudio();

                    float rms = RelativeAudioParser.RMS(trimmedBuffer);

                    //add result to moving average -- but only if we don't detect the speaker
                    if (!inNewSampleRecordingState) {
                        movingavg.add(rms, RelativeAudioParser.getCurrentBins());
                    } else {
                        movingavg.clearCandidate();
                    }

                    //update env noise
                    envNoiseLevel = movingavg.getAverage();
                    Log.d("train", "env noise " + envNoiseLevel);


                    //fill result
                    Log.d("Test","envnoiselevel: " + envNoiseLevel);
                    Log.d("Test", "Speaker frequency: " + RelativeAudioParser.getSpeakerFrequency() + ", Current frequency: " + RelativeAudioParser.getCurrentFrequency());

                    result = new ParseResult(ParseResult.ParseErrorCodes.TRAINING, rms, envNoiseLevel, inNewSampleRecordingState);
                    if (!inNewSampleRecordingState) {
                        RelativeAudioParser.resetCurrentBins();
                    }
                    else
                    {
                        RelativeAudioParser.subtractFromBins(movingavg.getNormalizedFftBins());
                    }
                    break;
                }
            }

            return result;
        }


        @Override
        protected void onPostExecute(final ParseResult result)
        {

            if (!envNoiseSet) {
                if (envNoiseLevel > 0) {
                    envNoiseSet = true;
                    Log.d("train", "before setText");
                    recordButton.setText("Record");
                    Log.d("train", "after setText");

                }
            }

            if (result != null)
            {
                String str;
                if (result.errorCode == ParseResult.ParseErrorCodes.SUCCESS)
                {
                    //test: is the RMS above or below threshold of env noise? if so, we want to vibrate watch
                    // if not, just log time and ambient noise level
                    // todo: think about if we should log ambient noise every like x minutes instead?

                    Log.d("ParseResult", "RMS: " + result.data + ", EnvNoise: " + result.envNoise + ", isMatch: " + result.speakerMatch);

                    //graph it
                    Date timestamp = Calendar.getInstance().getTime();
                    Long time = timestamp.getTime();
                    speakerVol.appendData(new DataPoint(time, result.data), true, 50);
                    envNoise.appendData(new DataPoint(time, envNoiseLevel), true, 50);

//                    Log.d("graph", "at time " + time + " grapphed speaker vol " + result.data + " and env noise " + envNoiseLevel);


                }
                else {
                    str = "Error: " + result.errorCode.toString();
                    Log.d("Result", str);

                }
            }
            else
                Log.d("Result", "Null result");

            if (!isCancelled())
            {
                //Now start the task again
                _task = new MonitorAudioTask();
                _task.execute(null, null, null);
            }
        }
    }
}
