package com.anthony.shakeitoff;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

//Ads

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";

    private AdView mAdView;

    private ShakeListener mShaker;
    private NoScopeListener mNoScoper;

    private boolean useFrontComera = false;
    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;

    MediaPlayer mp = null;

    Timer timer = null;
    class FlashTask extends TimerTask {
        private int count = 16;

        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final ImageView flashOverlay = (ImageView)findViewById(R.id.flash_overlay);
                    if (flashOverlay.getVisibility() == View.VISIBLE)
                        flashOverlay.setVisibility(View.GONE);
                    else
                    {
                        flashOverlay.setVisibility(View.VISIBLE);
                        mp.start();
                    }
                    count --;
                    if (count == 0) FlashTask.this.cancel();
                }
            });
        }
    };
    ImageView crossImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make the app fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        //Ads
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        crossImage = (ImageView)findViewById(R.id.crosshair);
        mp = MediaPlayer.create(this, R.raw.shot);

        // set up camera preview
        preview = (SurfaceView)findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // set up the shake listener and back button for after picture taken
        final ImageButton backButton = (ImageButton)findViewById(R.id.back_button);
        final ImageButton swapCameraButton = (ImageButton)findViewById(R.id.swap_camera_button);
        mShaker = new ShakeListener(this);
        mShaker.setListener(new ShakeListener.Listener () {
            public void on() { takePicture(); }
        });
        mNoScoper = new NoScopeListener(this);
        mNoScoper.setListener(new NoScopeListener.Listener () {
            public void on() {
                takePicture();
                crossImage.setVisibility(View.VISIBLE);
                crossImage.bringToFront();
            }

            public void display(String value) {
                final TextView info = (TextView)findViewById(R.id.info);
                info.setText(value);
            }
        });
        backButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (camera != null && cameraConfigured && !inPreview) startPreview();
                        backButton.setVisibility(View.GONE);
                        swapCameraButton.setVisibility(View.VISIBLE);
                        crossImage.setVisibility(View.GONE);
                    }
                }
        );
        swapCameraButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPreview();
                    useFrontComera = !useFrontComera;
                    camera.release();
                    camera = openCamera(useFrontComera);
                    try { camera.setPreviewDisplay(previewHolder); }
                    catch (IOException e) {}
                    startPreview();
                }
            }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdView.resume();
        mShaker.onResume(); mNoScoper.onResume();

        camera = openCamera(useFrontComera);

        final ImageButton backButton = (ImageButton)findViewById(R.id.back_button);
        if (backButton.getVisibility() == View.GONE) startPreview(); // only start the preview if we are not currently in viewing mode
    }

    @Override
    public void onPause() {
        stopPreview();
        camera.release(); camera = null;
        mShaker.onPause(); mNoScoper.onPause();
        mAdView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        mAdView.destroy();
        super.onDestroy();
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
            camera.setDisplayOrientation(90);
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

    private void takePicture() { // takes a picture and stops the image preview
        if (camera != null && cameraConfigured && inPreview) {
            inPreview = false;
            camera.takePicture(null, null, pictureSaver); // take picture with JPEG callback

            final ImageButton backButton = (ImageButton)findViewById(R.id.back_button);
            backButton.setVisibility(View.VISIBLE);
            final ImageButton swapCameraButton = (ImageButton)findViewById(R.id.swap_camera_button);
            swapCameraButton.setVisibility(View.GONE);

            timer = new Timer(); timer.schedule(new FlashTask(), 0, 100);
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
