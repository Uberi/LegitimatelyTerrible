package com.anthony.shakeitoff;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.hardware.Camera;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity implements SensorEventListener {
    public static final String TAG = "MainActivity";

    private ShakeListener mShaker;

    private boolean useFrontComera = false;
    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;

    private SensorManager cSensorManager;
    private float currentDegree = 0f;

    TextView tvHeading;

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

        //Text view that will tell the user what degree for testing
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        //initialize your android device sensor capabilities
        cSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // set up the shake listener and back button for after picture taken
        final ImageButton backButton = ((ImageButton)findViewById(R.id.back_button));
        backButton.setEnabled(false);
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () {
            public void onShake() { takePicture(); backButton.setEnabled(true); }
        });
        backButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (camera != null && cameraConfigured && !inPreview) startPreview();
                        backButton.setEnabled(false);
                    }
                }
        );

        ((ImageButton)findViewById(R.id.swap_camera_button)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (camera != null) { camera.release(); camera = null; }
                        useFrontComera = !useFrontComera;
                        camera = openCamera(useFrontComera);
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        cSensorManager.registerListener(this, cSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        mShaker.resume();

        camera = openCamera(useFrontComera);
        startPreview();
    }

    @Override
    public void onPause() {
        stopPreview();
        camera.release(); camera = null;

        mShaker.pause();
        cSensorManager.unregisterListener(this);
        super.onPause();
    }

    // get the largest preview size (by area) that still fits inside the layout
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null || size.width * size.height > result.width * result.height)
                    result = size;
            }
        }
        return result;
    }

    private boolean cameraConfigured = false;
    private boolean inPreview = false;
    private void initPreview(int width, int height) {
        if (camera != null && previewHolder.getSurface() != null) {
            try { camera.setPreviewDisplay(previewHolder); }
            catch (Throwable t) { Log.e(TAG, "Exception in setPreviewDisplay()", t); }

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }
    private void startPreview() {
        if (cameraConfigured && camera != null && !inPreview) {
            camera.startPreview(); inPreview = true;
        }
    }
    private void stopPreview() {
        if (cameraConfigured && camera != null && inPreview) {
            inPreview = false; camera.stopPreview();
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {}
        public void surfaceDestroyed(SurfaceHolder holder) {}
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { // recreate the preview when the screen dimensions change
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

    @Override
    public void onSensorChanged(SensorEvent event){
        float degree = Math.round(event.values[0]);
        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //not in use
    }

    private void takePicture() { // takes a picture and stops the image preview
        if (camera != null && cameraConfigured && inPreview) {
            inPreview = false;
            camera.takePicture(null, null, pictureSaver); // take picture with JPEG callback
        }
    }
    private Camera.PictureCallback pictureSaver = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // To be safe, you should check that the SDCard is mounted using Environment.getExternalStorageState() before doing this.

            // This location works best if you want the created images to be shared between applications and persist after your app has been uninstalled.
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ShakeItOff");

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(TAG, "Error creating picture directory");
                    return;
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File pictureFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            // add the picture to the gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(pictureFile);
            mediaScanIntent.setData(contentUri);
            MainActivity.this.sendBroadcast(mediaScanIntent);
        }
    };
}
