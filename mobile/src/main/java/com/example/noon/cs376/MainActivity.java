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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
    private boolean inTestingState = false;
    private float envNoiseLevel = -1; //from movin avg
    private float envFrequency = 0;
    private float LOUD_RATIO_THRESHOLD = 2.2f; //vibrate watch if x times softer/louder than env noise
    private float LOUD_MINIMUM_TRESHOLD = 800f;
    private boolean USE_WATCH_VIBRATION = false;
    private boolean USE_WOZ = false;
    private int samplesToDelay = 0;

    MainDatabase db;
    MainDao dao;

    WozDatabase wdb;
    WozDao wdao;
    WozResult lastWozResult;
    private int wozTrialId = 0;
    private int wozParticipantId = -1;
    private int wozConversationCondition = -1;
    private int wozVolumeCondition = -1;

    MovingAverage movingavg;
    //RelativeAudioParser parser;
    FFT fft;
    private static final int FFT_BINS = 2048;
    int fftLoopsPerBuffer;

    //graph stuff
    GraphView graph;
    String currWozGraph = "";
    LineGraphSeries<DataPoint> envNoise = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> speakerVol = new LineGraphSeries<>();
    PointsGraphSeries<DataPoint> loudIncidents = new PointsGraphSeries<>();
    PointsGraphSeries<DataPoint> speakingIncidents = new PointsGraphSeries<>();

    //welcome message display stuff
    private int timesTriggered = 0;
    private int quiet_incidents = 0;
    private int moderate_incidents = 0;
    private int loud_incidents = 0;
    private static final float QUIET_THRES = 100;
    private static final float MODERATE_THRES = 1000;
    private static final float LOUD_THRES = 1800;

    //for graph WoZ lines
    private static final float QUIET_TOP = 700;
    private static final float MODERATE_TOP = 2000;
    private static final float LOUD_TOP = 2500;

    LineGraphSeries<DataPoint> quietThres = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> moderateThres = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> loudThres = new LineGraphSeries<>();

    LineGraphSeries<DataPoint> quietTop = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> moderateTop = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> loudTop = new LineGraphSeries<>();


    //AlizeSpeechRecognizer alize;

    MainService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create database
        db = Room.databaseBuilder(getApplicationContext(),
                MainDatabase.class, "maindatabase").fallbackToDestructiveMigration().build();
        dao = db.mainDao();

        wdb = Room.databaseBuilder(getApplicationContext(),
                WozDatabase.class, "wozdatabase").fallbackToDestructiveMigration().build();
        wdao = wdb.wozDao();

        //link the buttons
        final Button trainAppButton = findViewById(R.id.get_started);
        trainAppButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){

                    startActivity(new Intent(getApplicationContext(),TrainingActivity.class));

                    return true;
                }
                return false;
            }
        });

        final Button learnMoreButton = findViewById(R.id.learn_more);
        learnMoreButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){

                    startActivity(new Intent(getApplicationContext(),LearnMoreActivity.class));

                    return true;
                }
                return false;
            }
        });

        //for vibration boolean
        ToggleButton toggle = findViewById(R.id.vib_mode);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    USE_WATCH_VIBRATION = true;
                } else {
                    USE_WATCH_VIBRATION = false;
                }
                Log.d("button", "Changed watch vibe to " + USE_WATCH_VIBRATION);

            }
        });

        //to disable normal vibrating if WoZ
        ToggleButton toggleWoZ = findViewById(R.id.woz_mode);
        toggleWoZ.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    USE_WOZ = true;
                    LinearLayout wozlayout = findViewById(R.id.WOZinterface);
                    wozlayout.setVisibility(View.VISIBLE);
                } else {
                    USE_WOZ = false;
                    LinearLayout wozlayout = findViewById(R.id.WOZinterface);
                    wozlayout.setVisibility(View.INVISIBLE);
                }
                Log.d("button", "Changed Woz to " + USE_WOZ);
            }
        });

        // ------------ buttons for WoZ ------------------ //
        final EditText participantIdTextView = findViewById(R.id.participantID);
        participantIdTextView.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    wozParticipantId = Integer.parseInt(participantIdTextView.getText().toString());
                    return true;
                }
                return false;
            }
        });

        Button undoTrialButton = findViewById(R.id.undoTrial);
        undoTrialButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozTrialId--;
                    TextView trialTextView = findViewById(R.id.trialID);
                    trialTextView.setText(""+wozTrialId);
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            wdao.delete(lastWozResult);
                        }
                    }).start();

                }
            }
        });

        Button shrekButton = findViewById(R.id.shrekConv);
        shrekButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozConversationCondition = WozResult.CONVERSATION_CONDITION__SHREK;
                    wozTrialId = 0;
                    TextView trialTextView = findViewById(R.id.trialID);
                    trialTextView.setText(""+wozTrialId);
                    Context context = getApplicationContext();
                    CharSequence text = "THIS IS MY SWAMP NOW";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }
            }
        });

        Button tfiosButton = findViewById(R.id.TFIOSConv);
        tfiosButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozConversationCondition = WozResult.CONVERSATION_CONDITION__TFIOS;
                    wozTrialId = 0;
                    TextView trialTextView = findViewById(R.id.trialID);
                    trialTextView.setText(""+wozTrialId);
                    Context context = getApplicationContext();
                    CharSequence text = "Conv: TFIOS";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }
            }
        });

        Button pbButton = findViewById(R.id.PBConv);
        pbButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozConversationCondition = WozResult.CONVERSATION_CONDITION__PB;
                    wozTrialId = 0;
                    TextView trialTextView = findViewById(R.id.trialID);
                    trialTextView.setText(""+wozTrialId);
                    Context context = getApplicationContext();
                    CharSequence text = "Conv: PB";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }
            }
        });

        Button quietButton = findViewById(R.id.quietWOZ);
        quietButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozVolumeCondition = WozResult.VOLUME_CONDITION__SILENT;
                    Context context = getApplicationContext();
                    CharSequence text = "Volume: Quiet";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();

                    //show graph
                    replaceWozGraph("quiet");
                    currWozGraph = "quiet";
                }
            }
        });

        Button mediumButton = findViewById(R.id.mediumWOZ);
        mediumButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozVolumeCondition = WozResult.VOLUME_CONDITION__MEDIUM;
                    Context context = getApplicationContext();
                    CharSequence text = "Volume: Medium";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();

                    //show graph
                    replaceWozGraph("moderate");
                    currWozGraph = "moderate";
                }
            }
        });

        Button noisyButton = findViewById(R.id.noisyWOZ);
        noisyButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    wozVolumeCondition = WozResult.VOLUME_CONDITION__NOISY;
                    Context context = getApplicationContext();
                    CharSequence text = "Volume: Noisy";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();

                    //show graph
                    replaceWozGraph("loud");
                    currWozGraph = "loud";
                }
            }
        });

        Button loudButton = findViewById(R.id.loudWOZ);
        loudButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    if (bound) {
                        if (USE_WATCH_VIBRATION) {
                            mService.sendMessage(MainService.PATH, "loud");
                        }

                        final WozResult newResult = new WozResult(wozParticipantId, wozTrialId, USE_WATCH_VIBRATION, wozVolumeCondition, wozConversationCondition, WozResult.SPEAKER_VOLUME__HIGH);
                        new Thread( new Runnable() {
                            @Override
                            public void run() {
                                wdao.insert(newResult);
                            }
                        }).start();
                        Log.d("WozResult", newResult.toString());
                        wozTrialId++;
                        TextView trialTextView = findViewById(R.id.trialID);
                        trialTextView.setText(""+wozTrialId);
                        /*
                        new Thread( new Runnable() {
                            @Override
                            public void run() {
                                List<WozResult> ids = wdao.getParticipantIds();
                                System.out.println("num ids: " + ids.size());
                            }
                        }).start();
                        */

                        lastWozResult = newResult;
                        Log.d("watch", "sent loud msg");
                    }
                }
            }
        });

        Button softButton = findViewById(R.id.softWOZ);
        softButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    if (bound) {
                        if (USE_WATCH_VIBRATION) {
                            mService.sendMessage(MainService.PATH, "soft");
                        }
                        final WozResult newResult = new WozResult(wozParticipantId, wozTrialId, USE_WATCH_VIBRATION, wozVolumeCondition, wozConversationCondition, WozResult.SPEAKER_VOLUME__LOW);
                        new Thread( new Runnable() {
                            @Override
                            public void run() {
                                wdao.insert(newResult);
                            }
                        }).start();
                        Log.d("WozResult", newResult.toString());
                        wozTrialId++;
                        TextView trialTextView = findViewById(R.id.trialID);
                        trialTextView.setText(""+wozTrialId);

                        lastWozResult = newResult;
                        Log.d("watch", "sent soft msg");
                    }
                }
            }
        });

        Button correctButton = findViewById(R.id.correctWOZ);
        correctButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (USE_WOZ) {
                    if (bound) {
                        final WozResult newResult = new WozResult(wozParticipantId, wozTrialId, USE_WATCH_VIBRATION, wozVolumeCondition, wozConversationCondition, WozResult.SPEAKER_VOLUME__CORRECT);
                        new Thread( new Runnable() {
                            @Override
                            public void run() {
                                wdao.insert(newResult);
                            }
                        }).start();
                        Log.d("WozResult", newResult.toString());
                        wozTrialId++;
                        TextView trialTextView = findViewById(R.id.trialID);
                        trialTextView.setText(""+wozTrialId);

                        lastWozResult = newResult;
                    }
                }
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

        RelativeAudioParser.Init(FFT_BINS);
        //parser = new RelativeAudioParser(FFT_BINS);
        //create a moving avg filter
        movingavg = new MovingAverage(MOVING_AVG_WINDOW_SIZE, FFT_BINS);

        //init graph
        graph = findViewById(R.id.graph);


        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
        String formattedDate = df.format(c.getTime());


        envNoise.setTitle("Environmental Noise");
        envNoise.setColor(Color.GREEN);
        envNoise.setThickness(4);

        loudIncidents.setColor(Color.RED);
        loudIncidents.setSize(14);
        speakingIncidents.setColor(Color.BLUE);
        speakingIncidents.setSize(14);

        speakerVol.setTitle("Your Volume");
        speakerVol.setColor(Color.BLUE);
        speakerVol.setThickness(8);

        graph.setTitle(formattedDate);

        quietThres.setColor(Color.RED);
        quietThres.setThickness(1);
        moderateThres.setColor(Color.RED);
        moderateThres.setThickness(1);
        loudThres.setColor(Color.RED);
        loudThres.setThickness(1);
        quietTop.setColor(Color.RED);
        quietTop.setThickness(1);
        moderateTop.setColor(Color.RED);
        moderateTop.setThickness(1);
        loudTop.setColor(Color.RED);
        loudTop.setThickness(1);


        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // return the hour
                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss");
                    return sdf.format(value);

                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        // set date label formatter
//        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getApplicationContext()));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);
//        graph.getGridLabelRenderer().setHumanRounding(false);

        //current time

        Calendar cal = Calendar.getInstance();

        Date currtime = cal.getTime();
        cal.add(Calendar.MINUTE, 1);
        Date futuretime = cal.getTime();

        Long start = currtime.getTime();
        Long stop = futuretime.getTime();


        graph.getViewport().setMinX(start);
        graph.getViewport().setMaxX(stop);
        graph.getViewport().setXAxisBoundsManual(true);


//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(0);
//        graph.getViewport().setMaxY(3000);

        graph.getViewport().setScalable(true);
//        graph.getViewport().setScalableY(true);

        graph.addSeries(envNoise);
        graph.addSeries(speakerVol);
        graph.addSeries(speakingIncidents);
        graph.addSeries(loudIncidents);




        // Hide WoZ layout on start
        LinearLayout wozlayout = findViewById(R.id.WOZinterface);
        wozlayout.setVisibility(View.INVISIBLE);
    }



    @Override
    protected void onStart() {
        super.onStart();
        // Create and bind service
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d("watch", "service started");
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
        if (!USE_WOZ) {
            // Get instance of Vibrator from current Context
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            // Vibrate for 400 milliseconds
            v.vibrate(VIBRATION_DURATION);
        }
    }

    private DataPoint[] getNewData (Long time, float value) {
        DataPoint[] v = new DataPoint[1];
        v[0] = new DataPoint(time, value);
        return v;
    }

    private void replaceWozGraph(String type) {
        Calendar cal = Calendar.getInstance();
        Date cur = cal.getTime();
        Long start = cur.getTime();
        cal.add(Calendar.MINUTE, 1); //2 min away for rendering line
        Date woztime = cal.getTime();
        Long wozstop = woztime.getTime();

        if (currWozGraph.equals("quiet")) {
            graph.removeSeries(quietThres);
            graph.removeSeries(quietTop);
            Log.d("graph", "removed quiet");
        } else if (currWozGraph.equals("moderate")) {
            graph.removeSeries(moderateThres);
            graph.removeSeries(moderateTop);
            Log.d("graph", "removed mod");
        } else if (currWozGraph.equals("loud")) {
            graph.removeSeries(loudThres);
            graph.removeSeries(loudTop);
            Log.d("graph", "removed loud");
        }

        if (type.equals("quiet")) {

            quietThres.resetData(getNewData(start, QUIET_THRES));
            quietThres.appendData(new DataPoint(wozstop, QUIET_THRES), false, 5);
            quietTop.resetData(getNewData(start, QUIET_TOP));
            quietTop.appendData(new DataPoint(wozstop, QUIET_TOP), false, 5);

            graph.addSeries(quietThres);
            graph.addSeries(quietTop);
            Log.d("graph", "add quiet");
        } else if (type.equals("moderate")) {
            moderateThres.resetData(getNewData(start, MODERATE_THRES));
            moderateThres.appendData(new DataPoint(wozstop, MODERATE_THRES), false, 5);
            moderateTop.resetData(getNewData(start, MODERATE_TOP));
            moderateTop.appendData(new DataPoint(wozstop, MODERATE_TOP), false, 5);

            graph.addSeries(moderateThres);
            graph.addSeries(moderateTop);
            Log.d("graph", "add moderate");

        } else if (type.equals("loud")) {
            loudThres.resetData(getNewData(start, LOUD_THRES));
            loudThres.appendData(new DataPoint(wozstop, LOUD_THRES), false, 5);
            loudTop.resetData(getNewData(start, LOUD_TOP));
            loudTop.appendData(new DataPoint(wozstop, LOUD_TOP), false, 5);

            graph.addSeries(loudThres);
            graph.addSeries(loudTop);
            Log.d("graph", "add loud");

        }
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
                if (isCancelled() || !RelativeAudioParser.isSpeakerFrequencySet()) {
                    Log.d("MainActivity", "Cancelling");
                    break;
                }

                bufferReadResult = _audioRecord.read(buffer, 0, BUFFER_SIZE);
                short[] trimmedBuffer = Arrays.copyOfRange(buffer, 0, bufferReadResult);
                if (bufferReadResult > 0)
                {
                    if (samplesToDelay == 0) {
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
                            RelativeAudioParser.addToBins(FFT.computeMagnitude(x,y));
                        }
                        //alize.addNewAudioSample(trimmedBuffer);

                        //byte[] audioBytes = new byte[2 * BUFFER_SIZE];
                        //ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buffer);
                        //alize.addNewAudioSample(audioBytes);

                        //alize.resetAudio();

                        float rms = RelativeAudioParser.RMS(trimmedBuffer);
                        boolean speakerMatch = RelativeAudioParser.isSpeakerMatch(movingavg.getNormalizedFftBins());

                        //add result to moving average -- but only if we don't detect the speaker
                        if (envNoiseLevel < 0 || !speakerMatch) {
                            movingavg.add(rms, RelativeAudioParser.getCurrentBins());
                        } else {
                            movingavg.clearCandidate();
                        }

                        //update env noise
                        envNoiseLevel = movingavg.getAverage();
                        envFrequency = movingavg.getFrequency();

                        //fill result

                        Log.d("Test", "Speaker frequency: " + RelativeAudioParser.getSpeakerFrequency() + ", Current frequency: " + RelativeAudioParser.getCurrentFrequency());

                        /*
                        System.out.print("Current bins are: ");
                        double[] currentBins = RelativeAudioParser.getCurrentBins();
                        for (double d : currentBins)
                        {
                            System.out.print(d + " ");
                        }
                        System.out.println();
                        */

                        result = new ParseResult(ParseResult.ParseErrorCodes.SUCCESS, rms, envNoiseLevel, speakerMatch);
                        RelativeAudioParser.resetCurrentBins();
                        
                    }
                    else {
                        samplesToDelay--;
                    }
                    break;
                }
            }

            return result;
        }

        private String calculateCommonEnvironment() {
            if (moderate_incidents >= quiet_incidents  && moderate_incidents >= loud_incidents) {
                return "moderately loud";
            } else if (loud_incidents > moderate_incidents) {
                return "loud";
            } else {
                return "quiet";
            }
        }


        private void incrementEnvironment(float envNoise) {
            if (envNoise < MODERATE_THRES) quiet_incidents += 1;
            else if (envNoise < LOUD_THRES) moderate_incidents += 1;
            else loud_incidents += 1;
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

                    Log.d("ParseResult", "RMS: " + result.data + ", EnvNoise: " + result.envNoise + ", isMatch: " + result.speakerMatch);

                    //graph it
                    Date timestamp = Calendar.getInstance().getTime();
                    Long time = timestamp.getTime();
                    speakerVol.appendData(new DataPoint(time, result.data), true, 70);
                    envNoise.appendData(new DataPoint(time, envNoiseLevel), true, 70);

//                    Log.d("graph", "at time " + time + " grapphed speaker vol " + result.data + " and env noise " + envNoiseLevel);


                    if (result.speakerMatch) {

                        float upper = envNoiseLevel * LOUD_RATIO_THRESHOLD;
                        float rms = result.data;
                        if (rms >= upper && rms >= LOUD_MINIMUM_TRESHOLD) {
                            //a hit!
                            //probably do speaker ID here
                            samplesToDelay = DELAY_SAMPLES_AFTER_VIBRATION;
                            //vibrate the watch and also not wizard
                            if (USE_WATCH_VIBRATION && !USE_WOZ) {
                                if (bound) {
                                    mService.sendMessage(MainService.PATH, Float.toString(result.data));
                                    Log.d("watch", "sent msg to watch in speech detection");
                                }
                            }
                            else {
                                triggerVibrate();
                            }

                            //add red point to graph

                            loudIncidents.appendData(new DataPoint(time, result.data), true, 30);

                            Log.d("Result", "LOUD: " + result.data + "\r\n");
                            timesTriggered += 1;
                            incrementEnvironment(envNoiseLevel);

                            //edit the textview
                            //TODO ADD SOFT TO THIS FOR WOZ
                            TextView welcome = findViewById(R.id.welcome_msg);
                            if (timesTriggered == 1) {
                                welcome.setText("Today, you spoke too loudly 1 time in a " + calculateCommonEnvironment() + " environment.");
                            } else {
                                welcome.setText("Today, you spoke too loudly " + timesTriggered + " times in a " + calculateCommonEnvironment() + " environment.");
                            }


                        } else {

                            speakingIncidents.appendData(new DataPoint(time, result.data), true, 30);
                            Log.d("Result", "Not loud: " + result.data + "\r\n");

                        }

                    }
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            dao.insert(result);
                        }
                    }).start();
                    // Insert into database
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
