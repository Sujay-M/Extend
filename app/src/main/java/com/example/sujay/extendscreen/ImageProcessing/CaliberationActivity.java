package com.example.sujay.extendscreen.ImageProcessing;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.example.sujay.extendscreen.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sujay on 16/8/15.
 */
public class CaliberationActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener
{
    private final String TAG = "CaliberationActivity";
    private List<Camera.Size> mResolutionList;
    private OpenCVCameraView mOpenCvCameraView;
    private Scalar minThresh,maxThresh;
    private short SENSITIVITY = 50;
    private boolean toggle = true;
    private Mat currentFrame,currentBlobs;
    private List<Rect> boundingBoxes = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
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
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.caliberation_activity);
        mOpenCvCameraView = (OpenCVCameraView)findViewById(R.id.my_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);

        minThresh = new Scalar(0,255-SENSITIVITY,0);
        maxThresh = new Scalar(255,255,255);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //init();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private void init()
    {
        double smallestPreviewSize = 1280 * 720;
        double smallestWidth = 480;
        mResolutionList = mOpenCvCameraView.getResolutionList();
        if(mResolutionList!=null)
        {
            Camera.Size selected = null;
            for (Camera.Size previewSize : mResolutionList)
            {
                if ((previewSize.height*previewSize.width) < smallestPreviewSize && previewSize.width >= smallestWidth)
                    selected = previewSize;
            }
            if(selected!=null)
                mOpenCvCameraView.setResolution(selected);
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        Log.d(TAG,"CameraViewStarted");
        toggle = true;
        currentFrame = new Mat(height, width, CvType.CV_8UC4);
        currentBlobs = new Mat(height, width, CvType.CV_8UC4);
        boundingBoxes = new ArrayList<Rect>();
    }

    @Override
    public void onCameraViewStopped()
    {
        Log.d(TAG,"CameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        currentFrame = inputFrame.rgba().clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.cvtColor(currentFrame, currentBlobs, Imgproc.COLOR_RGB2HLS);
        Core.inRange(currentBlobs, minThresh, maxThresh, currentBlobs);
        Imgproc.findContours(currentBlobs,contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        boundingBoxes.clear();
        for (int i=0; i<contours.size(); i++)
        {
            if(Imgproc.contourArea(contours.get(i))<1000)
                continue;
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );
            Rect rect = Imgproc.boundingRect(points);
            if(((float)rect.width/(float)rect.height)>1.7)
                continue;
            if((float)rect.width/(float)rect.height<.5)
                continue;

            boundingBoxes.add(rect);
            Core.rectangle(currentFrame, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2);

        }
        return currentFrame;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
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
    public boolean onTouch(View v, MotionEvent event)
    {
        if(toggle)
        {
            mOpenCvCameraView.disableView();
            toggle = false;

        }
        int cols = currentFrame.cols();
        int rows = currentFrame.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
        Log.d(TAG,"xOff= :"+xOffset+" yOff = :"+yOffset);
        Log.d(TAG,"x = :"+event.getX()+" y = :"+event.getY());
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;
        Point p = new Point(x,y);
        for (Rect cur:boundingBoxes)
        {
            if(cur.contains(p))
            {
                Log.d(TAG,"in a rectangle br = "+cur.br().toString()+" tl = "+cur.tl().toString());
            }
        }
        mOpenCvCameraView.setWillNotDraw(false);
        return true;
    }
}
