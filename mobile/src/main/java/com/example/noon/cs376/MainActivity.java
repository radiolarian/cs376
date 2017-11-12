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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private static final int FREQUENCY = 44100;
    //private static final double QUIET_THRESHOLD = 32768.0 * 0.02; //anything higher than 0.02% is considered non-silence
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int BUFFER_SIZE;
    private int RMS_WINDOW_SIZE;
    private AudioRecord _audioRecord;
    private AsyncTask<Void, Void, ParseResult> _task;
    private boolean bound = false;
    private boolean inTrainingState = false;

    MainDatabase db;
    MainDao dao;

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

        //link the button
        final Button button = findViewById(R.id.train);
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    inTrainingState = true;
                    Log.d("Alize", "Starting training");
                    // Do what you want
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    inTrainingState = false;
                    Log.d("Alize", "Ending training");
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
            BUFFER_SIZE = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING) * 8;
            _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY,
                    CHANNEL_CONFIG, AUDIO_ENCODING, BUFFER_SIZE);
            Log.d("Init", "AudioRecord set up successfully");

            // For now, let's set window size to buffer size (probably will need to adjust)
            RMS_WINDOW_SIZE = BUFFER_SIZE;
        }
        catch (Exception ex)
        {
            Log.d("Exception", ex.toString());
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //screen needs to be on for it to collect data

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
            Long bufferReadResult = null;
            ParseResult result = new ParseResult(ParseResult.ParseErrorCodes.CANCELLED, "");

            while (true)
            {
                if (isCancelled())
                    break;

                bufferReadResult = new Long(_audioRecord.read(buffer, 0, BUFFER_SIZE));
                if (bufferReadResult > 0)
                {
                    if (inTrainingState)
                    {
                        byte[] audioBytes = new byte[2 * BUFFER_SIZE];
                        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buffer);
                        alize.trainSpeakerModel(audioBytes);
                        alize.resetAudio();
                    }

                   result = new ParseResult(ParseResult.ParseErrorCodes.SUCCESS, RelativeAudioParser.RMS(buffer));
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
                    // For now, send the message to the watch for each update.
                    // We'll probably want to filter/smooth this result in the future.
                    if (bound) {
                        mService.sendMessage(MainService.PATH, result.data);
                    }
                    str = "Data: " + result.data + "\r\n";
                }
                else
                {
                    str = "Error: " + result.errorCode.toString();
                }

                Log.d("Result", str);
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
