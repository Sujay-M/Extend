package com.example.sujay.extendscreen.ImageProcessing;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.activities.StartServer;
import com.example.sujay.extendscreen.models.ClientModel;
import com.example.sujay.extendscreen.utils.Server;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sujay on 27/8/15.
 */
public class NewLayout extends Activity implements View.OnTouchListener, View.OnClickListener {
    static final int REQUEST_TAKE_PHOTO = 1;
    ImageView mImageView;
    private boolean isImageAvailable = false;
    Bitmap ORIGINAL = null;
    Button bNext;
    Mat originalImg = null;
    int curDevNo = 0;
    private boolean bitmapAvailable = false;
    boolean photoFlag = true;
    List<RotatedRect> boundingBoxes = new ArrayList<>();
    private final String TAG = "MAINACTIVITY";
    private Server server;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    if(bitmapAvailable && ORIGINAL!=null)
                    {
                        Log.d(TAG,"starting processing");
                        startProcessing();
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_layout);
        mImageView = (ImageView)findViewById(R.id.ivCaptured);
        mImageView.setOnTouchListener(this);
        findViewById(R.id.tvShow).setVisibility(View.VISIBLE);
        findViewById(R.id.bNLDone).setOnClickListener(this);
        findViewById(R.id.bPhoto).setOnClickListener(this);
        bNext = (Button)findViewById(R.id.bNLNext);
        bNext.setOnClickListener(this);
        bNext.setVisibility(View.INVISIBLE);
        server = Server.getSingleton();
        server.sendToAll("COMMAND WHITE");
    }

    @Override
    public void onPause()
    {
        super.onPause();

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
        server = Server.getSingleton();
    }

    public void onDestroy() {
        super.onDestroy();

    }

    private void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex)
            {

            }
            if (photoFile != null)
            {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    private File createImageFile() throws IOException
    {

        String imageFileName = "temp.png";
        File storageDir = Environment.getExternalStorageDirectory();
        File image = new File(storageDir,imageFileName);
        return image;
    }
    private void setImage(Bitmap bmp)
    {
        mImageView.setImageBitmap(bmp);
    }
    private void setImage(Mat img)
    {
        Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bm);
        setImage(bm);
    }
    private void setPic()
    {
        findViewById(R.id.tvShow).setVisibility(View.GONE);
        try
        {
            int targetW = mImageView.getWidth();
            int targetH = mImageView.getHeight();
            String path = Environment.getExternalStorageDirectory()+"/temp.png";
            File f = new File(path);
            InputStream is = new FileInputStream(f);
            Bitmap bmp = BitmapFactory.decodeStream(is);

            int width = bmp.getWidth();
            int height = bmp.getHeight();

            float xScale = ((float) targetW) / width;
            float yScale = ((float) targetH) / height;
            float scale = (xScale <= yScale) ? xScale : yScale;

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            ORIGINAL = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);

            setImage(ORIGINAL);
            isImageAvailable = true;
            bitmapAvailable = true;

        }
        catch (FileNotFoundException e)
        {

        }

    }
    private void startProcessing()
    {

        originalImg = new Mat();
        Bitmap image = ORIGINAL.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(image, originalImg);
        AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>()
        {
            final int SENSITIVITY = 50;
            final Scalar minThresh = new Scalar(0,255-SENSITIVITY,0);
            final Scalar maxThresh = new Scalar(255,255,255);
            @Override
            protected void onPostExecute(Void aVoid)
            {
                super.onPostExecute(aVoid);
                Log.d(TAG,"execution ended");
                setImage(originalImg);
                if(server.getNoOfClients()!=0)
                {
                    DatagramPacket pkt = server.buildPacket(curDevNo,"COMMAND RED");
                    server.sendToClient(curDevNo,pkt);
                }

            }

            @Override
            protected Void doInBackground(Void... params)
            {

                Log.d(TAG,"execution started");
                Mat imgHLS = new Mat();
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

                Imgproc.cvtColor(originalImg, imgHLS, Imgproc.COLOR_RGB2HLS);
                Core.inRange(imgHLS, minThresh, maxThresh, imgHLS);
                Imgproc.findContours(imgHLS,contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

                MatOfPoint2f approxCurve = new MatOfPoint2f();
                boundingBoxes.clear();
                for (int i=0; i<contours.size(); i++)
                {
                    MatOfPoint current = contours.get(i);
                    if(Imgproc.contourArea(current)<1000)
                        continue;
                    MatOfPoint2f contour2f = new MatOfPoint2f( current.toArray() );
                    double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                    RotatedRect rrect = Imgproc.minAreaRect(approxCurve);
                    Rect rect = rrect.boundingRect();
                    if((Imgproc.contourArea(current))<(rect.area()-1000))
                        if(((float)rect.width/(float)rect.height)>1.7)
                            continue;
                    if((float)rect.width/(float)rect.height<.5)
                        continue;

                    Point[] vertices = new Point[4];
                    rrect.points(vertices);
                    for (int j = 0; j < 4; j++)
                        Core.line(originalImg, vertices[j], vertices[(j+1)%4], new Scalar(0,255,0));

                    boundingBoxes.add(rrect);
                    Core.rectangle(originalImg, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2);

                }
                return null;
            }
        };
        if(Build.VERSION.SDK_INT >= 11)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK)
        {
            curDevNo = 0;
            setPic();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {

        if(isImageAvailable)
        {
            if(curDevNo<server.getNoOfClients())
            {
                int cols = ORIGINAL.getWidth();
                int rows = ORIGINAL.getHeight();

                int xOffset = (mImageView.getWidth() - cols) / 2;
                int yOffset = (mImageView.getHeight() - rows) / 2;

                Log.d(TAG,"coordinates = ("+event.getX()+","+event.getY()+")");

                Log.d(TAG,"Offsets x = "+xOffset+" y = "+yOffset);

                int x = (int)event.getX() - xOffset;
                int y = (int)event.getY() - yOffset;
                Point p = new Point(x,y);
                for (RotatedRect cur:boundingBoxes)
                {
                    if(cur.boundingRect().contains(p))
                    {
                        Log.d(TAG,"in a rectangle");
                        assignBox(cur);
                    }
                }
            }

        }

        return true;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.bNLNext:
                if(curDevNo>server.getNoOfClients())
                {
                    Toast.makeText(this,"press done",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    RotatedRect box = server.getClient(curDevNo).getRectangle();
                    Rect r = box.boundingRect();
                    Core.rectangle(originalImg, r.tl(), r.br(), new Scalar(0, 255, 0), -1);
                    setImage(originalImg);
                    curDevNo+=1;
                    if(curDevNo<server.getNoOfClients())
                    {
                        DatagramPacket pkt = server.buildPacket(curDevNo,"COMMAND RED");
                        server.sendToClient(curDevNo,pkt);
                    }

                }
                bNext.setVisibility(View.INVISIBLE);
                break;
            case R.id.bNLDone:
                Intent i = new Intent(this,StartServer.class);
                startActivity(i);
                finish();
                break;
            case R.id.bPhoto:
                dispatchTakePictureIntent();
                ((Button)findViewById(R.id.bPhoto)).setText("RETAKE");
                break;
        }
    }
    private void assignBox(RotatedRect box)
    {
        ClientModel c = server.getClient(curDevNo);
        if(c!=null)
        {
            c.setRectangle(box);
        }
        Rect r = box.boundingRect();
        Mat img = originalImg.clone();
        Core.rectangle(img, r.tl(), r.br(), new Scalar(255, 255, 0), -1);
        setImage(img);
        bNext.setVisibility(View.VISIBLE);
    }



}
