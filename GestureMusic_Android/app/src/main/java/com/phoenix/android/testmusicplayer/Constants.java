package com.phoenix.android.testmusicplayer;

/**
 * Constants used for communication between components of the handheld application. For example,
 * the Main UI can send messages to start/stop the sensor service.
 *
 * @see SensorService
 */
class Constants {
    public interface ACTION {
        String START_FOREGROUND = "edu.umass.cs.mygestures.action.start-foreground";
        String STOP_FOREGROUND = "edu.umass.cs.mygestures.action.stop-foreground";

        String BROADCAST_MESSAGE = "edu.umass.cs.my-activities-toolkit.action.broadcast-message";

        String BROADCAST_STATUS = "edu.umass.cs.my-activities-toolkit.action.broadcast-status";



        String BROADCAST_DATA_FROM_SERVER = "data-from-server";

        String BROADCAST_ANDROID_STEP_COUNT = "edu.umass.cs.my-activities-toolkit.action.broadcast-android-step-count";

        String BROADCAST_LOCAL_STEP_COUNT = "edu.umass.cs.my-activities-toolkit.action.broadcast-local-step-count";

        String BROADCAST_ACCELEROMETER_PEAK = "edu.umass.cs.my-activities-toolkit.action.broadcast-accelerometer-peak";
    }

    public interface MESSAGE {
        int REGISTER_CLIENT = 0;
        int UNREGISTER_CLIENT = 1;
        int SENSOR_STARTED = 2;
        int SENSOR_STOPPED = 3;
        int STATUS = 4;
    }

    public interface KEY {
        String STATUS = "edu.umass.cs.mygestures.key.status";
        String MESSAGE = "message";
        String VALUE = "value";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }

    public interface MHLClientFilter {
        String STEP_DETECTED = "STEP_DETECTED";
        String ACTIVITY_DETECTED = "GESTURE_DETECTED";
        String SPEAKER_DETECTED = "SPEAKER_DETECTED";
        String CLUSTER = "CLUSTER";
    }
}