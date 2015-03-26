package com.anthony.shakeitoff;

/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class NoScopeListener implements SensorEventListener {
    private static final int TURN_THRESHOLD = 15; // minimum change in heading before considering the change a turn
    private static final int NOSCOPE_COOLDOWN = 300; // amount of time to disable sensor checking for after each detection
    private static final double HEADING_THRESHOLD = 45; // maximum closeness to the target heading before considering it a detection

    private boolean[] circleCheckpoints = new boolean[4];
    private boolean isDetecting = false;
    private double targetHeading = 0;
    private double lastHeading = 0;
    private long lastTime = 0;
    private long lastTurnTime = 0;

    public interface Listener { public void on(); public void display(String value); }
    private Listener listener;
    public void setListener(Listener listener) { this.listener = listener; }

    private SensorManager sensorManager;
    private Context context;
    public NoScopeListener(Context context) {
        this.context = context;
        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        onResume();
    }

    public void onResume() {
        Sensor orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);
    }
    public void onPause() {
        Sensor orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.unregisterListener(this, orientation);
    }

    private boolean detectNoScope(double angle, double angleTarget) {
        // fill in the correct checkpoint
        double interval = 360 / circleCheckpoints.length;
        for(int i = 0; i < circleCheckpoints.length; i++)
            if (angle > interval * i && angle < interval * (i + 1))
                circleCheckpoints[i] = true;

        //String value = String.valueOf(circleCheckpoints[0]) + " " + String.valueOf(circleCheckpoints[1]) + " " + String.valueOf(circleCheckpoints[2]) + " " + String.valueOf(circleCheckpoints[3]) +
                //"    " + String.valueOf(absoluteAngleDifference(angle, angleTarget)) + " " + angleTarget + " " + angle;
        //listener.display(value);

        // check if all checkpoints are hit
        boolean fullCircle = true;
        for(boolean visited : circleCheckpoints) if (!visited) fullCircle = false;
        if (!fullCircle) return false;

        // check if we are currently at the right heading
        if (absoluteAngleDifference(angle, angleTarget) < HEADING_THRESHOLD) {
            for(int i = 0; i < circleCheckpoints.length; i++) // reset checkpoints
                circleCheckpoints[i] = false;
            return true;
        }
        return false;
    }

    private double absoluteAngleDifference(double angle1, double angle2) {
        double difference1 = angle1 - angle2, difference2 = angle2 - angle1;
        if (difference1 < 0) difference1 += 360; if (difference2 < 0) difference2 += 360;
        return Math.min(difference1, difference2);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastTime < 100) return;
        lastTime = now;

        double azimuth = event.values[0], pitch = event.values[1], roll = event.values[2];

        // restart the no scope detection when the user stops turning for a while
        if (absoluteAngleDifference(lastHeading, azimuth) < TURN_THRESHOLD) {
            if (now - lastTurnTime > NOSCOPE_COOLDOWN) { // did not turn for duration of cooldown
                isDetecting = true;
                targetHeading = azimuth; // reset detection
                for(int i = 0; i < circleCheckpoints.length; i++) // reset checkpoints
                    circleCheckpoints[i] = false;
            }
        }
        else {
            lastHeading = azimuth;
            lastTurnTime = now;
        }

        if (isDetecting && detectNoScope(azimuth, targetHeading)) {
            isDetecting = false;
            lastTurnTime = now;
            listener.on();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
