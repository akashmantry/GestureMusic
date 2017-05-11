package com.phoenix.android.testmusicplayer;

import android.app.Activity;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.devadvance.circularseekbar.CircularSeekBar;

import com.pheelicks.utils.TunnelPlayerWorkaround;
import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.BarGraphRenderer;
import android.graphics.Color;
import android.graphics.Paint;

public class MainActivity extends Activity {

    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private Button btnStart;
    private Button btnStop;
    private CircularSeekBar songProgressBar;
    private  MediaPlayer mp;
    private Handler myHandler = new Handler();
    private double startTime = 0;
    private double finalTime = 0;
    //private int seekForwardTime = 5000; // 5000 milliseconds
    //private int seekBackwardTime = 5000; // 5000 milliseconds

    private AudioManager audioManager;
    int curVolume, maxVolume, volume;
    private float previous_received_value = 0;
    private int maxDistance = 2000;
    private int minDistance = 0;
    private static int songIndex = 0;
    private boolean song_playing = false;


    private TextView songTitle,songCurrentDurationLabel;
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    private VisualizerView mVisualizerView;
    private MediaPlayer mSilentPlayer;  /* to avoid tunnel player issue */

    private ArrayList<String> listOfFilePath = new ArrayList<String>();

    /** used for debugging purposes */
    private static final String TAG = MainActivity.class.getName();
    String text = null;

    /**
     * Messenger service for exchanging messages with the background service
     */
    private Messenger mService = null;
    /**
     * Variable indicating if this activity is connected to the service
     */
    private boolean mIsBound = false;
    /**
     * Messenger receiving messages from the background service to update UI
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));



    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_DATA_FROM_SERVER)) {
                    String message = intent.getStringExtra(Constants.KEY.MESSAGE);
                    //String value = intent.getStringExtra(Constants.KEY.VALUE);
                    float value = intent.getFloatExtra(Constants.KEY.VALUE, 0);
                    displayAccelerometerReading(message, value);
                }
            }
        }
    };

    // Function for volume change
    private void changeVolume(String message, float oldValue) {
        int minVolume = 0;
        int maxVolume = 0;
        int currentVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "Current Volume: " + currentVolume);
        if (message.equals("volume_up")){
            minVolume = currentVolume;
            maxVolume = 15;
        } else if (message.equals("volume_down")) {
            maxVolume = currentVolume;
            minVolume = 0;
        }

        int oldRange = (maxDistance - minDistance);
        int newRange = (maxVolume - minVolume);
        float newVolume = (((oldValue - minDistance) * newRange) / oldRange) + minVolume;

        Log.d(TAG, "oldRange" + oldRange);
        Log.d(TAG, "newRange" + newRange);
        Log.d(TAG, "oldValue" + oldValue);
        Log.d(TAG, "minVolume" + minVolume);
        Log.d(TAG, "maxVolume" + maxVolume);
        Log.d(TAG, "New Volume: " + (int) newVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) newVolume, 1);
    }


    /**
     * Receive input from BandServices
     */
    private void displayAccelerometerReading(final String message, final float input){
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (previous_received_value != input) {
                    previous_received_value = input;
                    Log.d(TAG, "Message received: " + message);
                    if (message.equals("volume_up") || message.equals("volume_down")) {
                        changeVolume(message, input);
                    } else if (message.equals("play_pause")){
                        play_pause();
                    } else if(message.equals("next_song") || message.equals("previous_song")){
                        change_song(message);
                    }
                }
            }
        });
    }

    /**
     * Handler to handle incoming messages
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<MainActivity> mMainActivity;

        IncomingHandler(MainActivity mainActivity) {
            mMainActivity = new WeakReference<>(mainActivity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.SENSOR_STARTED:
                {
                    mMainActivity.get().updateStatus("sensor started.");
                    break;
                }
                case Constants.MESSAGE.SENSOR_STOPPED:
                {
                    mMainActivity.get().updateStatus("sensor stopped.");
                    break;
                }
                case Constants.MESSAGE.STATUS:
                {
                    mMainActivity.get().updateStatus(msg.getData().getString(Constants.KEY.STATUS));
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Connection with the service
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            updateStatus("Attached to the sensor service.");
            mIsBound = true;
            try {
                Message msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mIsBound = false;
            mService = null;
            updateStatus("Disconnected from the sensor service.");
        }
    };


    private void bindToServiceIfIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (SensorService.isRunning()) {
            doBindService();//
            updateStatus("Request to bind service");
        }
    }

    /**
     * Binds the activity to the background service
     */
    void doBindService() {
        bindService(new Intent(this, BandServices.class), mConnection, Context.BIND_AUTO_CREATE);
        updateStatus("Binding to Service...");
    }

    /**
     * Unbind this activity from the background service
     */
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            updateStatus("Unbinding from Service...");
        }
    }

    // Function for play/pause
    void play_pause(){
        if (mp.isPlaying()) {
            mp.pause();
            btnPlay.setImageResource(R.drawable.btn_play);
            mVisualizerView.clearRenderers();
            song_playing = false;
        } else {
            btnPlay.setImageResource(R.drawable.btn_pause);
            mp.start();
            song_playing = true;
            finalTime = mp.getDuration();
            startTime = mp.getCurrentPosition();

            songProgressBar.setMax((int) finalTime);
            songCurrentDurationLabel.setText(String.format("%d min, %d sec",
                            TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                            TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                            startTime)))
            );

            songProgressBar.setProgress((int) startTime);
            myHandler.postDelayed(UpdateSongTime, 100);

            addBarGraphRenderers();
        }
    }

    public void checkSongIndex(){
        if(songIndex > listOfFilePath.size())
            songIndex = 0;
        else if (songIndex < 0)
            songIndex = listOfFilePath.size() - 1;
    }

    //Function to change the song
    public void change_song(String change){
        try {
            mp.reset();

            if (change.equals("next_song"))
                ++songIndex;
            else if ((change.equals("previous_song")))
                --songIndex;

            checkSongIndex();
            mp.setDataSource(this, Uri.parse(listOfFilePath.get(songIndex)));
            setSongTitle(listOfFilePath.get(songIndex));
            mp.prepare();
            if(song_playing)
                mp.start();
        } catch(IOException e){
            Log.d(TAG, "" + e);
        }
        startTime = 0;

        songProgressBar.setProgress((int) startTime);
        songCurrentDurationLabel.setText(String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                        toMinutes((long) startTime)))
        );
    }

    ArrayList<String> findSongs(String rootPath) {
        ArrayList<String> fileList = new ArrayList<>();
        try{
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles(); //here you will get NPE if directory doesn't contains  any file,handle it like this.
            for (File file : files) {
                if (file.isDirectory()) {
                    if (findSongs(file.getAbsolutePath()) != null) {
                        fileList.addAll(findSongs(file.getAbsolutePath()));
                    } else {
                        break;
                    }
                } else if (file.getName().endsWith(".mp3")) {
                    fileList.add(file.getAbsolutePath());
                }
            }
            return fileList;
        }catch(Exception e){
            return null;
        }
    }

    // Set song title
    public void setSongTitle(String filePath){
        mmr.setDataSource(filePath);
        String songName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        songTitle.setText(songName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        if(findSongs("/storage/sdcard1/")!=null){
            listOfFilePath=findSongs("/storage/sdcard1/");
            //Log.d(TAG, "SD card's first music file: " + listOfFilePath.get(0));
        }else {
            Toast.makeText(this, "SD Card not available!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "sdCard not available");
        }


        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnForward = (ImageButton) findViewById(R.id.btnForward);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        songProgressBar = (CircularSeekBar) findViewById(R.id.songProgressBar);
        songTitle = (TextView)findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView)findViewById(R.id.songCurrentDurationLabel);
        setSongTitle(listOfFilePath.get(songIndex));

        bindToServiceIfIsRunning();

        // Audio manager stuff
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        /**
         * Start sending data from band
         * */
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBound) {
                    doBindService();
                }
                if (mIsBound) {

                    LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Constants.ACTION.BROADCAST_DATA_FROM_SERVER);
                    broadcastManager.registerReceiver(receiver, filter);

                    Intent startIntent = new Intent(MainActivity.this, BandServices.class);
                    startIntent.setAction(Constants.ACTION.START_FOREGROUND);
                    Log.d(TAG, "text: " + text);
                    startIntent.putExtra("pass", text);
                    startService(startIntent);
                }
            }
        });

        /**
         * Stop sending data from band
         * */
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBound) {
                    doBindService();
                }
                if (mIsBound) {
                    Intent stopIntent = new Intent(MainActivity.this, BandServices.class);
                    stopIntent.setAction(Constants.ACTION.STOP_FOREGROUND);
                    startService(stopIntent);
                }
            }
        });

        // Set the first song from the list
        mp = MediaPlayer.create(this, Uri.parse(listOfFilePath.get(songIndex)));

        /**
         * Play/pause button click event
         * Play/pause song
         * */
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                play_pause();
            }
        });

        /**
         * Forward button click event
         * Go to next song
         * */
        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                change_song("next_song");
            }
        });

        /**
         * Backward button click event
         * Got to previous song
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                change_song("previous_song");

            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                btnPlay.setImageResource(R.drawable.btn_play);
                startTime = 0;

                songProgressBar.setProgress((int) startTime);
                songCurrentDurationLabel.setText(String.format("%d min, %d sec",
                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                );

                change_song("next_song");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initTunnelPlayerWorkaround();
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        mVisualizerView.link(mp);
        addBarGraphRenderers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cleanUp();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
        super.onDestroy();
        cleanUp();
    }

    private void cleanUp()
    {
        if (mp != null)
        {
            mVisualizerView.release();
        }

        if (mSilentPlayer != null)
        {
            mSilentPlayer.release();
            mSilentPlayer = null;
        }
    }

    private void initTunnelPlayerWorkaround() {
        // Read "tunnel.decode" system property to determine
        // the workaround is needed
        if (TunnelPlayerWorkaround.isTunnelDecodeEnabled(this)) {
            mSilentPlayer = TunnelPlayerWorkaround.createSilentMediaPlayer(this);
        }
    }

    // Methods for adding renderers to visualizer
    private void addBarGraphRenderers()
    {
        Paint paint2 = new Paint();
        paint2.setStrokeWidth(30f);
        paint2.setAntiAlias(true);
        paint2.setColor(Color.rgb(255, 120, 0));
        BarGraphRenderer barGraphRendererTop = new BarGraphRenderer(8, paint2, false);
        mVisualizerView.addRenderer(barGraphRendererTop);
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mp.getCurrentPosition();
            songCurrentDurationLabel.setText(String.format("%d min, %d sec",
                            TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                            TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                            toMinutes((long) startTime)))
            );
            songProgressBar.setProgress((int)startTime);
            myHandler.postDelayed(this, 100);
        }
    };

    /**
     * Updates the status message on the main UI
     * @param string the new status message
     */
    private void updateStatus(final String string) {
/*        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });*/
        //Log.d(TAG, string);
    }
}