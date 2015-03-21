package com.anthony.shakeitoff;

/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.util.Log;

import java.lang.UnsupportedOperationException;

public class ShakeListener implements SensorListener
{
    private static final double FORCE_THRESHOLD = 0.3;
    private static final int TIME_THRESHOLD = 100; // time between shake checks
    private static final int SHAKE_COOLDOWN = 1000; // time after each shake to wait without detecting shakes

    private SensorManager mSensorMgr;
    private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private long mLastShake;

    public interface OnShakeListener
    {
        public void onShake();
    }

    public ShakeListener(Context context)
    {
        mContext = context;
        resume();
    }

    public void setOnShakeListener(OnShakeListener listener)
    {
        mShakeListener = listener;
    }

    public void resume() {
        mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = mSensorMgr.registerListener(this, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
        if (!supported) {
            mSensorMgr.unregisterListener(this, SensorManager.SENSOR_ACCELEROMETER);
            throw new UnsupportedOperationException("Accelerometer not supported");
        }
    }

    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, SensorManager.SENSOR_ACCELEROMETER);
            mSensorMgr = null;
        }
    }

    public void onAccuracyChanged(int sensor, int accuracy) { }

    public void onSensorChanged(int sensor, float[] values)
    {
        if (sensor != SensorManager.SENSOR_ACCELEROMETER) return;
        long now = System.currentTimeMillis();
        long diff = now - mLastTime;
        if (diff > TIME_THRESHOLD) {
            double speed = Math.sqrt(
                Math.pow(values[SensorManager.DATA_X] - mLastX, 2) +
                Math.pow(values[SensorManager.DATA_Y] - mLastY, 2) +
                Math.pow(values[SensorManager.DATA_Z] - mLastZ, 2)
            ) / diff;
            if (speed > FORCE_THRESHOLD) {
                if (now - mLastShake > SHAKE_COOLDOWN) {
                    mLastShake = now;
                    if (mShakeListener != null) {
                        mShakeListener.onShake();
                    }
                }
            }
            mLastTime = now;
            mLastX = values[SensorManager.DATA_X];
            mLastY = values[SensorManager.DATA_Y];
            mLastZ = values[SensorManager.DATA_Z];
        }
    }
}
