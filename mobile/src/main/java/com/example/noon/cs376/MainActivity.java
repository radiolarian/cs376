package com.example.noon.cs376;

import android.arch.persistence.room.Room;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int MOVING_AVG_WINDOW_SIZE = 5; //num of audio samples to determine BG vol
    private static final int FREQUENCY = 8000;
    //private static final double QUIET_THRESHOLD = 32768.0 * 0.02; //anything higher than 0.02% is considered non-silence
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIN_BUFFER_SIZE_MULTIPLIER = 1;
    private int BUFFER_SIZE;
    private int RMS_WINDOW_SIZE;
    private AudioRecord _audioRecord;
    private AsyncTask<Void, Void, ParseResult> _task;
    private boolean bound = false;
    private boolean inNewSampleRecordingState = false;
    private float envNoiseLevel; //from movin avg
    private float TRIGGER_THRESHOLD = 1.5f; //vibrate watch if x times softer/louder than env noise

    MainDatabase db;
    MainDao dao;

    MovingAverage movingavg;
    AlizeSpeechRecognizer alize;

    MainService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create database
        db = Room.databaseBuilder(getApplicationContext(),
                MainDatabase.class, "database-name").build();
        dao = db.mainDao();

        // Create Alize client
        alize = new AlizeSpeechRecognizer(getApplicationContext());

        //link the buttons
        final Button addSampleButton = findViewById(R.id.add_sample);
        addSampleButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    inNewSampleRecordingState = true;
                    Log.d("Alize", "Recording a new sample...");
                    // Do what you want
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    inNewSampleRecordingState = false;
                    Log.d("Alize", "Ending new sample recording");
                    // Do what you want
                    return true;
                }

                return false;
            }
        });
        final Button trainButton = findViewById(R.id.train);
        trainButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d("Alize", "Starting training");
                    alize.trainModel();
                    // Do what you want
                    return true;
                }
                return false;
            }
        });

        final Button testButton = findViewById(R.id.test_model);
        testButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d("Alize", "Starting testing");
                    alize.testModel();
                    // Do what you want
                    return true;
                }
                return false;
            }
        });

        // Set up audio buffer
        try
        {
            Log.d("Init", "Setting up AudioRecord");
            BUFFER_SIZE = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING) * 8 * MIN_BUFFER_SIZE_MULTIPLIER;
            _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY,
                    CHANNEL_CONFIG, AUDIO_ENCODING, BUFFER_SIZE);
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

        //create a moving avg filter
        movingavg = new MovingAverage(MOVING_AVG_WINDOW_SIZE);

        //init graph TODO replace this
        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);

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

    private class MonitorAudioTask extends AsyncTask<Void, Void, ParseResult>
    {
        @Override
        protected ParseResult doInBackground(Void... params)
        {
            short[] buffer = new short[BUFFER_SIZE];
            int bufferReadResult = 0;
            ParseResult result = new ParseResult(ParseResult.ParseErrorCodes.CANCELLED, "", -1);

            while (true)
            {
                if (isCancelled())
                    break;

                bufferReadResult = _audioRecord.read(buffer, 0, BUFFER_SIZE);
                short[] trimmedBuffer = Arrays.copyOfRange(buffer, 0, bufferReadResult);
                if (bufferReadResult > 0)
                {
                    if (inNewSampleRecordingState)
                    {
                        alize.addNewAudioSample(trimmedBuffer);

                        //byte[] audioBytes = new byte[2 * BUFFER_SIZE];
                        //ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buffer);
                        //alize.addNewAudioSample(audioBytes);
                        
                        //alize.resetAudio();
                    }
                    float rms = RelativeAudioParser.RMS(trimmedBuffer);
                    //add result to moving average
                    movingavg.add(rms);
                    //update env noise
                    envNoiseLevel = movingavg.getAverage();
                    Log.d("Result", "Env noise: " + Float.toString(envNoiseLevel) + "\r\n");

                    //fill result

                    result = new ParseResult(ParseResult.ParseErrorCodes.SUCCESS, Float.toString(rms), envNoiseLevel);

                   break;
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ParseResult result)
        {
            if (result != null)
            {
                String str;
                if (result.errorCode == ParseResult.ParseErrorCodes.SUCCESS)
                {
                    //test: is the RMS above or below threshold of env noise? if so, we want to vibrate watch
                    // if not, just log time and ambient noise level
                    // todo: think about if we should log ambient noise every like x minutes instead?
                    float upper = envNoiseLevel * TRIGGER_THRESHOLD;
                    float lower = envNoiseLevel / TRIGGER_THRESHOLD;
                    float rms = Float.parseFloat(result.data);
                    if (rms >= upper || rms <= lower ) {
                        //a hit!
                        //probably do speaker ID here

                        //vibrate the watch
                        if (bound) {
                            mService.sendMessage(MainService.PATH, result.data);
                        }

                        Log.d("Result", "Over thres!" + result.data + "\r\n");

                    } else {
                        Log.d("Result", "Under" + result.data + "\r\n");
                        result.data = "";
                        //TODO debate this w team - should we send empty or just send val? (if send val probs need a boolean entry then)

                    }

                }
                else
                {
                    str = "Error: " + result.errorCode.toString();
                    Log.d("Result", str);

                }

                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        dao.insert(result);
                    }
                }).start();
                // Insert into database
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
