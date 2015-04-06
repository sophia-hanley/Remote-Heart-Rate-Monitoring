package com.example.mrmac.remoteheartratemonitoring;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.VideoView;
import android.widget.MediaController;
import android.view.SurfaceView;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;


import java.io.IOException;
import java.util.List;

import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;


import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import android.hardware.Camera;
import android.content.res.Configuration;

import android.content.Context;
import android.util.Log;
import android.graphics.Paint;

import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.hardware.Camera;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.view.View;
import android.graphics.Color;






public class heartRate extends ActionBarActivity {

    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;

    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);

        mPreview = (TextureView) findViewById(R.id.videoView);
        captureButton = (Button) findViewById(R.id.but);
        setCaptureButtonText("Record"); 

    }

    /**
     * The capture button controls all user interaction. When recording, the button click
     * stops recording, releases {@link android.media.MediaRecorder} and {@link android.hardware.Camera}. When not recording,
     * it prepares the {@link android.media.MediaRecorder} and starts recording.
     *
     * @param view the view generating the event.
     */
    public void click(View view) {
        // Do stuff
    } {
        if (isRecording) {

            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            setCaptureButtonText("Record");
            isRecording = false;
            releaseCamera();

        } else {


            isRecording = true;


            new MediaPrepareTask().execute(null, null, null);


        }
    }

    private void setCaptureButtonText(String title) {
        captureButton.setText(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder(){

        mCamera=Camera.open(1);
        mCamera.setDisplayOrientation(90);

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);

        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }


        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
                CameraHelper.MEDIA_TYPE_VIDEO).toString());

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                //isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                heartRate.this.finish();
            }
            // inform the user that recording has started
            if(isRecording)
                setCaptureButtonText("Stop");

        }
    }

}


    /*private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
//                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //current_frame = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };*/

/*    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(1); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }*/
/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        recording = false;

        setContentView(R.layout.activity_heart_rate);

        //Get Camera for preview
        myCamera = getCameraInstance();
        if (myCamera == null) {
            Toast.makeText(heartRate.this,
                    "Fail to get Camera",
                    Toast.LENGTH_LONG).show();
        }

        myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
        //this is how you rotate to be portrait
        myCamera.setDisplayOrientation(90);
        FrameLayout myCameraPreview = (FrameLayout) findViewById(R.id.videoView);
        myCameraPreview.addView(myCameraSurfaceView);

        myButton = (Button) findViewById(R.id.but);

        Button.OnClickListener myButtonOnClickListener
                = new Button.OnClickListener(){
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(recording){
                    // stop recording and release camera
                    mediaRec.stop();  // stop the recording
                    releaseMediaRecorder(); // release the MediaRecorder object

                    //Exit after saved
                    finish();
                }else{

                    //Release Camera before MediaRecorder start
                    releaseCamera();

                    if(!prepareMediaRecorder()){
                        Toast.makeText(heartRate.this,
                                "Fail in prepareMediaRecorder()!\n - Ended -",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    mediaRec.start();
                    recording = true;
                    myButton.setText("STOP");
                }
            }};
        myButton.setOnClickListener(myButtonOnClickListener);

        /*super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);
        //this will set everything up for opencv
        initializeOpenCVDependencies();
        onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCVView);
        //front facing camera
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener((CvCameraViewListener2) heartRate.this);//wtf why can't i make this work
        */

        /*else
        {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            {
                p.set("orientation", "portrait");
                p.set("rotation", 90);
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                p.set("orientation", "landscape");
                p.set("rotation", 90);
            }
        }
    }
/*

    private void releaseCamera() {
        if (myCamera != null) {
            myCamera.release();        // release the camera for other applications
            myCamera = null;
        }
    }

    /*public void onPause(){
        super.onPause();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();

    }
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();
    }
*//*
    private boolean prepareMediaRecorder() {
        myCamera = getCameraInstance();
        mediaRec = new MediaRecorder();

        myCamera.unlock();
        mediaRec.setCamera(myCamera);

        mediaRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRec.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);


        mediaRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mediaRec.setOutputFile("/sdcard/myvideo");
        mediaRec.setMaxDuration(60000); // Set max duration 60 sec.
        mediaRec.setMaxFileSize(5000000); // Set max file size 5M

        mediaRec.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());

        try {
            mediaRec.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private void releaseMediaRecorder() {
        if (mediaRec != null) {
            mediaRec.reset();   // clear recorder configuration
            mediaRec.release(); // release the recorder object
            mediaRec = null;
            myCamera.lock();           // lock camera for later use
        }
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    /*public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
        return mRgbaT; }*/

   /* public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public MyCameraSurfaceView(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int weight,
                                   int height) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // make any resize, rotate or reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub

        }


/*        private void initializeOpenCVDependencies() {


            try {
                // Copy the resource into a temp file so OpenCV can load it
                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                FileOutputStream os = new FileOutputStream(mCascadeFile);


                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();


                // Load the cascade classifier
                //cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e("OpenCVActivity", "Error loading cascade", e);
            }

        }*/

        /*@Override
        public void onResume() {
            super.onResume();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        }

        public void getInit() {
            videoView = (VideoView) findViewById(R.id.kittyCat);
            mC = new MediaController(this);
            dM = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(dM);
            int height = dM.heightPixels;
            int width = dM.widthPixels;
            videoView.setMinimumWidth(width);
            videoView.setMinimumHeight(height);
            videoView.setMediaController(mC);
            videoView.setVideoPath("sdcard/cats");//this will have whatever is on the sd card
            videoView.start();


        }*/

        /*@Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_heart_rate, menu);
            return true;
        }


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            }

            return super.onOptionsItemSelected(item);
        }



        this is for face detection to be implemented later
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
             // Create a grayscale image
             Mat aInputFrame=inputFrame.rgba();
             Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);


             MatOfRect faces = new MatOfRect();


             // Use the classifier to detect faces
             if (cascadeClassifier != null) {
                 cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                         new Size(absoluteFaceSize, absoluteFaceSize), new Size());
             }


             // If there are any faces found, draw a rectangle around it
             Rect[] facesArray = faces.toArray();
             for (int i = 0; i <facesArray.length; i++)
                 Core.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);


             return aInputFrame;
         }
    }
}
*/
