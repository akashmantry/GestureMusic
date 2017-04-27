package com.phoenix.android.testmusicplayer;

import android.app.Activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.devadvance.circularseekbar.CircularSeekBar;

import com.pheelicks.utils.TunnelPlayerWorkaround;
import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.BarGraphRenderer;
import com.pheelicks.visualizer.renderer.CircleBarRenderer;
import com.pheelicks.visualizer.renderer.CircleRenderer;
import com.pheelicks.visualizer.renderer.LineRenderer;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;

public class MainActivity extends Activity {

    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private Button btnStart;
    private Button btnStop;
    //private SeekBar songProgressBar;
    private CircularSeekBar songProgressBar;
    private  MediaPlayer mp;
    private Handler myHandler = new Handler();
    private double startTime = 0;
    private double finalTime = 0;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds

    private AudioManager audioManager;
    int curVolume, maxVolume, volume;
    private float previous_received_value = 0;
    private float currentVolume = 0.5f;
    private static final int maxVolume_volume_up = 26;
    private static final int minVolume_volume_down = 13;


    private TextView songTitle,songCurrentDurationLabel;

    private VisualizerView mVisualizerView;
    private MediaPlayer mSilentPlayer;  /* to avoid tunnel player issue */


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


    /**
     * Displays the server reading to the screen
     * TODO: have to map the value and use it to increase or decrease the volume
     */
    private void displayAccelerometerReading(final String message, final float input){
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (previous_received_value != input) {
                    previous_received_value = input;
                    Log.d(TAG, message);

                    //Toast.makeText(getApplicationContext(), message + input, Toast.LENGTH_SHORT).show();
                    if (message.equals("volume_up")) {
                        //float log1=(float)(1 - (Math.log(maxVolume_volume_up-input)/Math.log(maxVolume_volume_up)));
                        //float changedVolume = log1;
                        int currentVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float newVolume = currentVolume + (0.57f * input);
                        //Log.d(TAG, "log1: " + log1);
                        Log.d(TAG, "currentVolume: " + newVolume);
                        //currentVolume = curVolume + changedVolume*10;

                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) newVolume, 1);
                        //Log.d(TAG, "Volume up" + (int)newVolume);

                    } else if (message.equals("volume_down")) {
                        //float log1=(float)(1 - (Math.log(minVolume_volume_down-input)/Math.log(minVolume_volume_down)));
                        //float changedVolume = log1;
                        int currentVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float newVolume = currentVolume - (1.15f * input);
                        //Log.d(TAG, "log1: " + log1);
                        Log.d(TAG, "changedVolume: " + newVolume);
                        //currentVolume = curVolume - changedVolume*10;

                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)newVolume, 1);
                        //Log.d(TAG, "Volume down" + (int)currentVolume);
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnForward = (ImageButton) findViewById(R.id.btnForward);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        songProgressBar = (CircularSeekBar) findViewById(R.id.songProgressBar);
        //songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
        //songProgressBar.setClickable(false);

        songTitle = (TextView)findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView)findViewById(R.id.songCurrentDurationLabel);
        songTitle.setText("Song.mp3");

        bindToServiceIfIsRunning();

        // audio manager stuff
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "Max volume: " + maxVolume);

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

        mp = MediaPlayer.create(this, R.raw.song);
        //mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        //mVisualizerView.link(mp);
        //addBarGraphRenderers();

        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mp.isPlaying()) {
                    mp.pause();
                    Toast.makeText(getApplicationContext(), "Pausing sound", Toast.LENGTH_SHORT).show();
                    btnPlay.setImageResource(R.drawable.btn_play);

                    mVisualizerView.clearRenderers();
                } else {
                    btnPlay.setImageResource(R.drawable.btn_pause);
                    Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
                    mp.start();

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
        });

        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // get current song position
                int currentPosition = mp.getCurrentPosition();
                // check if seekForward time is lesser than song duration
                if(currentPosition + seekForwardTime <= mp.getDuration()){
                    // forward song
                    mp.seekTo(currentPosition + seekForwardTime);
                    Toast.makeText(getApplicationContext(),"You have Jumped forward 5 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Cannot jump forward 5 seconds",Toast.LENGTH_SHORT).show();
                }
            }
        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // get current song position
                int currentPosition = mp.getCurrentPosition();
                // check if seekBackward time is greater than 0 sec
                if(currentPosition - seekBackwardTime >= 0){
                    // backward song
                    mp.seekTo(currentPosition - seekBackwardTime);
                    Toast.makeText(getApplicationContext(),"You have Jumped backward 5 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    // backward to starting position
                    Toast.makeText(getApplicationContext(),"Cannot jump backward 5 seconds",Toast.LENGTH_SHORT).show();
                }

            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                btnPlay.setImageResource(R.drawable.btn_play);
                startTime = 0;

                songProgressBar.setProgress((int) startTime);
                //songVolumeBar.setProgress(50);
                songCurrentDurationLabel.setText(String.format("%d min, %d sec",
                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                );
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
/*        Paint paint = new Paint();
        paint.setStrokeWidth(50f);
        paint.setAntiAlias(true);
        paint.setColor(Color.argb(200, 56, 138, 252));
        BarGraphRenderer barGraphRendererBottom = new BarGraphRenderer(16, paint, false);
        mVisualizerView.addRenderer(barGraphRendererBottom);*/

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