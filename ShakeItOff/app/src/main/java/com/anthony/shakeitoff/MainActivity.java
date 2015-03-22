package com.anthony.shakeitoff;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.Calendar;

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

    //checks the direction of spinning
    private SensorManager cSensorManager;
    private boolean canSpin = false;
    private float currentDegree = 0f;
    private float prevDegree = 0f;
    private int prevTime = 0;

    //crosshair image
    ImageView crossImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make the app fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        crossImage = (ImageView)findViewById(R.id.crosshair);
        // set up camera preview
        preview = (SurfaceView)findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //initialize your android device sensor capabilities
        cSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // set up the shake listener and back button for after picture taken
        final ImageButton backButton = (ImageButton)findViewById(R.id.back_button);
        final ImageButton swapCameraButton = (ImageButton)findViewById(R.id.swap_camera_button);
        backButton.setEnabled(false);
        swapCameraButton.setEnabled(true);
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () {
            public void onShake() { takePicture(); }
        });
        backButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (camera != null && cameraConfigured && !inPreview) startPreview();
                        backButton.setEnabled(false);
                        swapCameraButton.setEnabled(true);
                        crossImage.setVisibility(View.INVISIBLE);
                    }
                }
        );
        swapCameraButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (camera != null) {
                            camera.release();
                            camera = null;
                        }
                        useFrontComera = !useFrontComera;
                        camera = openCamera(useFrontComera);
                        inPreview = false;
                        startPreview();
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


    //MOD FUNCTIONS ARE REQUIRED IN ORDER TO WORK WITH DEGREES
    /*private float modDeg(float deg){
        deg = deg % 360;
        if(deg < 0){
            deg = deg + 360;
        }
        return deg;
    }*/
    private boolean detectRoll = false;
    private boolean[] checkpointsR = new boolean[4];
    private boolean fullRollTurn = false;

    private void setDetectRoll(boolean detectRoll){
        this.detectRoll = detectRoll;
    }

    private boolean areAllTrue(boolean[] array){
        for(boolean b : array)
            if (!b)
                return false;
        return true;
    }


    private void detectingRoll(){
        setDetectRoll(true);
        for(int i = 0; i < 4; i++){
            if((currentDegree > 90 * i && currentDegree < 90 * (i + 1))){
                checkpointsR[i] = true;
            }
        }
        if(areAllTrue(checkpointsR) && currentDegree > 0 && currentDegree > 45){
            fullRollTurn = true;
            //reset checkpoints
            for(int i = 0; i < 4; i++){
                checkpointsR[i] = false;
            }
        }
        setDetectRoll(false);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        float degree = Math.round(event.values[0]);
        Calendar c = Calendar.getInstance();
        int curSeconds = c.get(Calendar.SECOND);
        if((prevDegree > degree - 5) && (prevDegree < degree + 5)) {
            if (prevTime + 2 < curSeconds) {
                canSpin = true;
            }
        }
        else {
            prevDegree = degree;
        }

        if(canSpin){
            detectingRoll();
        }
        if(fullRollTurn){
            canSpin = false;
            takePicture();
            crossImage.setVisibility(View.VISIBLE);
            crossImage.bringToFront();
            fullRollTurn = false;
        }
        //I don't know why this is here so im commenting it out
        currentDegree = degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //not in use
    }

    private void takePicture() { // takes a picture and stops the image preview
        if (camera != null && cameraConfigured && inPreview) {
            inPreview = false;
            camera.takePicture(null, null, pictureSaver); // take picture with JPEG callback

            final ImageButton backButton = (ImageButton)findViewById(R.id.back_button);
            backButton.setEnabled(true);
            final ImageButton swapCameraButton = (ImageButton)findViewById(R.id.swap_camera_button);
            swapCameraButton.setEnabled(false);
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
