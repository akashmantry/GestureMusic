package com.phoenix.android.testmusicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

import edu.umass.cs.MHLClient.client.ConnectionStateHandler;
import edu.umass.cs.MHLClient.client.MobileIOClient;
import edu.umass.cs.MHLClient.structures.BlockingSensorReadingQueue;

/**
 * The BandService is responsible for starting and stopping the sensors on the Band and receiving
 * accelerometer and gyroscope data periodically. It is a foreground service, so that the user
 * can close the application on the phone and continue to receive data from the wearable device.
 * Because the {@link BandGyroscopeEvent} also receives accelerometer readings, we only need to
 * register a {@link BandGyroscopeEventListener} and no {@link BandAccelerometerEventListener}.
 * This should be compatible with both the Microsoft Band and Microsoft Band 2.
 *
 * @author Sean Noran
 *
 * @see Service#startForeground(int, Notification)
 * @see BandClient
 * @see BandGyroscopeEventListener
 */
public abstract class SensorService extends Service implements ConnectionStateHandler {

    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient bandClient = null;

    /** Messenger used by clients */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /** List of bound clients/activities to this service */
    private ArrayList<Messenger> mClients = new ArrayList<>();

    /** indicates whether the sensor service is running or not */
    private static boolean isRunning = false;

    protected final String userID = "";
    private volatile BlockingSensorReadingQueue sensorReadingQueue;

    protected MobileIOClient mClient;

    public String received = null;

    /**
     * Handler to handle incoming messages
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<SensorService> mService;

        IncomingHandler(SensorService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.REGISTER_CLIENT:
                    mService.get().mClients.add(msg.replyTo);
                    break;
                case Constants.MESSAGE.UNREGISTER_CLIENT:
                    mService.get().mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Sends a status message to all clients, removing any inactive clients if necessary.
     * @param status the status message
     */
    protected void sendStatusToClients(String status) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                b.putString(Constants.KEY.STATUS, status);
                Message msg = Message.obtain(null, Constants.MESSAGE.STATUS);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    protected static boolean isRunning(){
        return isRunning;
    }

    protected void connectToServer(){
        mClient = MobileIOClient.getInstance(this, userID);
        mClient.setConnectionStateHandler(this);
        mClient.connect();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, intent.getAction());
        if (intent.getAction().equals(Constants.ACTION.START_FOREGROUND)){
            isRunning = true;

            Bundle extras = intent.getExtras();
            if(extras == null) {
                received= null;
            } else {
                received = extras.getString("pass");
            }
            Log.d(TAG, "received: "+received);

            // create option to stop the service from the notification
            Intent stopIntent = new Intent(this, SensorService.class);

            //stopIntent.setAction(Constants.ACTION.STOP_FOREGROUND);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            // notify the user that the foreground service has started
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.msg_service_started))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setOngoing(true)
                    .setVibrate(new long[]{0, 50, 150, 200})
                    .setPriority(Notification.PRIORITY_HIGH)
                    .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), pendingIntent).build();

            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);

            connectToServer();
            startSensors();
        } else if (intent.getAction().equals(Constants.ACTION.STOP_FOREGROUND)) {
            isRunning = false;
            unregisterSensors();
            disconnectBand();
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT);
            //if (writer != null)
            //    FileUtil.closeWriter(writer);
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    public String getReceived(){
        return received;
    }

    public void unregisterSensors(){}
    public void disconnectBand(){}
    /**
     * registers the band's accelerometer/gyroscope sensors to the sensor service
     */
    public void startSensors() {
        //
    }

    @Override
    public void onConnectionFailed(Exception e) {
        e.printStackTrace();
        Log.d(TAG, "Connection attempt failed.");
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "Connected to server");
    }

}