package com.phoenix.android.testmusicplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

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
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.umass.cs.MHLClient.client.MessageReceiver;

/**
 * Created by Akash on 14-03-2017.
 */
public class BandServices extends SensorService implements BandGyroscopeEventListener {

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient bandClient = null;

    /** used for debugging purposes */
    private static final String TAG = BandServices.class.getName();

    /**writing to csv file */
    private String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    private String fileName = "data.csv";
    private String filePath = baseDir + File.separator + fileName;
    static List<String[]> data_list = new ArrayList<String[]>();
    CSVWriter writer;

    public void writeToFile(){
        try{
            Log.d(TAG, "Started writing to file");
            writer = new CSVWriter(new FileWriter(filePath)); //create a writer object
            writer.writeAll(data_list);
            writer.close();
            Log.d(TAG, "Completed writing to file");
        }catch (IOException e){
            Log.e("Error writing", e.getMessage());
        }
    }

    /**
     * Asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors.
     * Errors may arise if the Band does not support the Band SDK version or the Microsoft Health
     * application is not installed on the mobile device.
     **
     * @see com.microsoft.band.BandErrorType#UNSUPPORTED_SDK_VERSION_ERROR
     * @see com.microsoft.band.BandErrorType#SERVICE_ERROR
     * @see BandClient#getSensorManager()
     * @see com.microsoft.band.sensors.BandSensorManager
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    sendStatusToClients(getString(R.string.status_connected));
                    bandClient.getSensorManager().registerGyroscopeEventListener(BandServices.this, SampleRate.MS16);
                } else {
                    sendStatusToClients(getString(R.string.status_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getString(R.string.err_unsupported_sdk_version);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getString(R.string.err_service);
                        break;
                    default:
                        exceptionMessage = getString(R.string.err_default) + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);
                sendStatusToClients(exceptionMessage);

            } catch (Exception e) {
                sendStatusToClients(getString(R.string.err_default) + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Connects the mobile device to the Microsoft Band
     * @return True if successful, False otherwise
     * @throws InterruptedException if the connection is interrupted
     * @throws BandException if the band SDK version is not compatible or the Microsoft Health band is not installed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                sendStatusToClients(getString(R.string.status_not_paired));
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            return true;
        }

        sendStatusToClients(getString(R.string.status_connecting));
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    /**
     * registers the band's accelerometer/gyroscope sensors to the sensor service
     */
    public void startSensors() {
        new SensorSubscriptionTask().execute();
    }

    /**
     * unregisters the sensors from the sensor service
     */
    public void unregisterSensors() {
        if (bandClient != null) {
            try {
                bandClient.getSensorManager().unregisterAllListeners();
                disconnectBand();
                //writeToFile();  //write to file when unregistering the ssensors
            } catch (BandIOException e) {
                sendStatusToClients(getString(R.string.err_default) + e.getMessage());
            }
        }
    }

    /**
     * disconnects the sensor service from the Microsoft Band
     */
    public void disconnectBand() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        Object[] data = new Object[]{event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()};
        // TODO: Send accelerometer readings to server and UI
        String sample = TextUtils.join(",", data);
        //Log.d(TAG, sample);
        float acc_x = event.getAccelerationX();
        float acc_y = event.getAccelerationY();
        float acc_z = event.getAccelerationZ();
        float angv_x = event.getAngularVelocityX();
        float angv_y = event.getAngularVelocityY();
        float angv_z = event.getAngularVelocityZ();
        //Calendar c = Calendar.getInstance();
        //long milliseconds = c.get(Calendar.MILLISECOND);
        String milli = Long.toString(event.getTimestamp());

        mClient.sendSensorReading(new AcceGyro(userID, "MOBILE_ANDROID", event.getTimestamp(), acc_x, acc_y, acc_z, angv_x, angv_y, angv_z));
        //Log.d(TAG, "label: "+received);
        //if(received=="Nothing" || received == null) {
        data_list.add(new String[]{milli, String.format("%.3f", acc_x), String.format("%.3f", acc_y), String.format("%.3f", acc_z), String.format("%.3f", angv_x), String.format("%.3f", angv_y), String.format("%.3f", angv_z)});
        //}else{
        //    data_list.add(new String[]{milli, String.format("%.3f", acc_x), String.format("%.3f", acc_y), String.format("%.3f", acc_z), String.format("%.3f", angv_x), String.format("%.3f", angv_y), String.format("%.3f", angv_z), received});
        //}
        //Log.d(TAG, "TimeStamp: " + milli);
        //adding data to the array list
        //3.27 1.99
    }

    @Override
    public void onConnected() {
        super.onConnected();
        mClient.registerMessageReceiver(new MessageReceiver(Constants.MHLClientFilter.ACTIVITY_DETECTED) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                Log.d(TAG, "Received message from server.");
                try {
                    JSONObject data = json.getJSONObject("data");
                    //long timestamp = data.getLong("timestamp");
                    String a = data.toString();
                    String[] separated = a.split(":");
                    String message = separated[0].substring(2, separated[0].length() - 1);
                    String value = separated[1].substring(0, separated[1].length() - 1);
                    float value_float = Float.parseFloat(value);
                    Log.d(TAG, "Message: " + message);
                    Log.d(TAG, "Value: " + value_float);
                    broadcastAccelerometerReading(message, value_float);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**

     * Broadcasts the accelerometer reading to other application components, e.g. the main UI.

     * @param message, value

     */

    public void broadcastAccelerometerReading(final String message, final float value) {

        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.MESSAGE, message);
        intent.putExtra(Constants.KEY.VALUE, value);
        intent.setAction(Constants.ACTION.BROADCAST_DATA_FROM_SERVER);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);

    }
}
