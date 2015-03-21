package com.anthony.shakeitoff;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.hardware.Camera;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
    }

    private Camera openCamera(boolean frontCamera) {
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if ((frontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) ||
                (!frontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
                try { cam = Camera.open(i); }
                catch (RuntimeException e) { Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage()); }
            }
        }
        return cam;
    }
}
