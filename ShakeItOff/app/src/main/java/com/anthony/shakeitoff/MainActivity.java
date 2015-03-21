package com.anthony.shakeitoff;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.hardware.Camera;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;
    private boolean cameraConfigured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make the app fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        // set up camera preview
        preview = (SurfaceView)findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onResume() {
        super.onResume();

        camera = openCamera(false);
        startPreview(); inPreview = true;
    }

    @Override
    public void onPause() {
        if (inPreview) { camera.stopPreview(); inPreview = false; }

        camera.release(); camera = null;

        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null || size.width * size.height > result.width * result.height)
                    result = size;
            }
        }
        return result;
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try { camera.setPreviewDisplay(previewHolder); }
            catch (Throwable t) { Log.e(TAG, "Exception in setPreviewDisplay()", t); }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height, parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured=true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.startPreview(); inPreview = true;
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {}
        public void surfaceDestroyed(SurfaceHolder holder) {}
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            initPreview(width, height);
            startPreview();
        }
    };

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
