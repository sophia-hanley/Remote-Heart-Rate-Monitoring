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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.util.Log;



public class heartRate extends ActionBarActivity {
    boolean video=false;
    boolean camera=true;
    private CameraBridgeViewBase mOpenCvCameraView;
    VideoView videoView;
    DisplayMetrics dM;
    SurfaceView sV;
    MediaController mC;
    private Mat grayscaleImage;
    private CascadeClassifier cascadeClassifier;
    private int absoluteFaceSize;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
//                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
//                    current_frame = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);
        if(video) {
            getInit();
        }
        else if(camera){
            initializeOpenCVDependencies();
            onResume();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCVView);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCameraIndex(1);



            // mOpenCvCameraView.setCameraIndex(1); this is how you save an image


        }
    }

    private void initializeOpenCVDependencies() {


        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
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
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

    }
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void getInit(){
        videoView=(VideoView)findViewById(R.id.kittyCat);
        mC=new MediaController(this);
        dM=new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dM);
        int height=dM.heightPixels;
        int width=dM.widthPixels;
        videoView.setMinimumWidth(width);
        videoView.setMinimumHeight(height);
        videoView.setMediaController(mC);
        videoView.setVideoPath("sdcard/cats");//this will have whatever is on the sd card
        videoView.start();


    }

    @Override
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

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        // Create a grayscale image
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
