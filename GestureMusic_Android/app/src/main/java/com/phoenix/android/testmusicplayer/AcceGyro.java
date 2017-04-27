package com.phoenix.android.testmusicplayer;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * Wraps an accelerometer reading and defines a JSON structure that allows
 * the reading to be sent to the server.
 *
 * @author Erik Risinger
 *
 * @see SensorReading
 */
public class AcceGyro extends SensorReading {

    /** The acceleration along the x-axis **/
    private final double acc_x;

    /** The acceleration along the y-axis **/
    private final double acc_y;

    /** The acceleration along the z-axis **/
    private final double acc_z;

    /** The angular velocity along the x-axis **/
    private final double ang_vel_x;

    /** The angular velocity along the y-axis **/
    private final double ang_vel_y;

    /** The angular velocity along the z-axis **/
    private final double ang_vel_z;

    /**
     * Instantiates an accelerometer reading.
     * @param userID a 10-byte hex string identifying the current user.
     * @param deviceType describes the device
     * param deviceID unique device identifier
     * @param t the timestamp at which the event occurred, in Unix time by convention.
     * values the x, y, z readings
     */
    public AcceGyro(String userID, String deviceType, long t, float acc_x, float acc_y, float acc_z, float ang_vel_x, float ang_vel_y, float ang_vel_z){
        super(userID, deviceType, "akash", "SENSOR_ACCEL", t);

        this.acc_x = acc_x;
        this.acc_y = acc_y;
        this.acc_z = acc_z;
        this.ang_vel_x = ang_vel_x;
        this.ang_vel_y = ang_vel_y;
        this.ang_vel_z = ang_vel_z;
    }

    /**
     * Instantiates an accelerometer reading.
     * @param userID a 10-byte hex string identifying the current user.
     * @param deviceType describes the device.
     * param deviceID unique device identifier.
     * @param t the timestamp at which the event occurred, in Unix time by convention.
     * @param label the class label associated with the reading.
     * values the x, y, z readings.
     */
    public AcceGyro(String userID, String deviceType, long t, int label, float acc_x, float acc_y, float acc_z, float ang_vel_x, float ang_vel_y, float ang_vel_z){
        super(userID, deviceType, "akash", "SENSOR_ACCEL", t, label);

        this.acc_x = acc_x;
        this.acc_y = acc_y;
        this.acc_z = acc_z;
        this.ang_vel_x = ang_vel_x;
        this.ang_vel_y = ang_vel_y;
        this.ang_vel_z = ang_vel_z;
    }

    @Override
    protected JSONObject toJSONObject(){
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("a_x", acc_x);
            data.put("a_y", acc_y);
            data.put("a_z", acc_z);
            data.put("av_x", ang_vel_x);
            data.put("av_y", ang_vel_y);
            data.put("av_z", ang_vel_z);

            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}

