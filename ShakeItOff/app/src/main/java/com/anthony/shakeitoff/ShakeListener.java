package com.anthony.shakeitoff;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;

public class ShakeListener implements SensorEventListener {
    private static final double ACCELERATION_CHANGE_THRESHOLD = 0.3; // minimum jerk (m/s^3) to be considered as a shake
    private static final int SHAKE_COOLDOWN = 1000; // time after each shake to wait without detecting shakes

    private float lastX = 0, lastY = 0, lastZ = 0;
    private long lastTime;
    private long lastShakeTime;

    public interface Listener { public void on(); }
    private Listener listener;
    public void setListener(Listener listener) { this.listener = listener; }

    private SensorManager sensorManager;
    private Context context;
    public ShakeListener(Context context) {
        this.context = context;
        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        onResume();
    }

    public void onResume() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }
    public void onPause() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        long now = System.currentTimeMillis();
        if (now - lastTime < 100) return; // duration was too short to get a good result
        double acceleration_change = Math.sqrt(
            Math.pow(event.values[0] - lastX, 2) +
            Math.pow(event.values[1] - lastY, 2) +
            Math.pow(event.values[2] - lastZ, 2)
        ) / (now - lastTime);
        if (acceleration_change > ACCELERATION_CHANGE_THRESHOLD) {
            if (now - lastShakeTime > SHAKE_COOLDOWN) {
                lastShakeTime = now;
                if (listener != null) {
                    listener.on();
                }
            }
        }
        lastTime = now;
        lastX = event.values[0]; lastY = event.values[1]; lastZ = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
