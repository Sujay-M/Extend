package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.models.DeviceModel;
import com.example.sujay.extendscreen.utils.Client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by sujay on 5/8/15.
 */
public class StartClient extends Activity implements Client.CommandFromServer, TextureView.SurfaceTextureListener {
    private final static String TAG = "StartClient";
    public String FILE_URL;

    private FrameLayout mainView;

    private Client c = null;
    private TextView tvMessageReceived;
    private InetAddress serverIp;
    private MediaPlayer mMediaPlayer;
    private TextureView mTextureView;
    private DeviceModel dev;
    float[] values = new float[8];
    private boolean mediaPrepared = false;
    private long clockSkew;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_activity);
        mainView = (FrameLayout)findViewById(R.id.flMainView);
        tvMessageReceived = (TextView)findViewById(R.id.tvMessageReceived);
        serverIp = (InetAddress) getIntent().getExtras().get("ServerIP");
        requestServer();
    }


    @Override
    public void commandReceived(String type, String data)
    {
        tvMessageReceived.setText("TYPE: "+type+"  DATA:"+data);
        String dataParts[] = data.split(" ");
        if(dataParts[0].equals("DEVNO"))
        {
            clockSkew = Long.parseLong(dataParts[1]);
        }
        if(type.equals("COMMAND"))
        {
            long serverCurrent,current,skewed;
            switch(dataParts[0])
            {
                case "WHITE":
                    mainView.setBackgroundColor(Color.WHITE);
                    break;
                case "RED":
                    mainView.setBackgroundColor(Color.RED);
                    break;
                case "PLAY":
                    serverCurrent = Long.parseLong(dataParts[1]);
                    Log.d(TAG,"play");
                    if(mediaPrepared)
                    {
                        current = SystemClock.elapsedRealtime();
                        skewed = serverCurrent+clockSkew;
                        try {
                            Thread.sleep(current-skewed+500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mMediaPlayer.start();
                    }
                    break;
                case "PAUSE":
                    serverCurrent = Long.parseLong(dataParts[1]);
                    if(mediaPrepared)
                    {

                        current = SystemClock.elapsedRealtime();
                        current = SystemClock.elapsedRealtime();
                        skewed = serverCurrent+clockSkew;
                        try {
                            Thread.sleep(current-skewed+500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();}
                        mMediaPlayer.pause();
                    }
                    break;
                case "STOP":
                    serverCurrent = Long.parseLong(dataParts[1]);
                    if(mediaPrepared)
                    {
                        current = SystemClock.elapsedRealtime();
                        mMediaPlayer.stop();
                    }
                    break;
                case "SEEK":
                    serverCurrent = Long.parseLong(dataParts[1]);
//                    if(mediaPrepared)

                    break;
                case "PREPARE":
                    mMediaPlayer.prepareAsync();
                    break;

            }
        }
        else if(type.equals("DATA"))
        {
            switch (dataParts[0])
            {
                case "CALIB":
                    switch (dataParts[1])
                    {
                        case "1":
                            setTextureView(1,0.0f,0.5f,0.0f,1.0f,0.2f,1.0f,0.2f,1.0f);
                            break;
                        case "2":
                            setTextureView(1,0.0f,0.5f,0.0f,1.0f,0.0f,1.0f,0.157f,0.842f);
                            break;
                        case "3":
                            setTextureView(1,0.5f,1.0f,0.0f,1.0f,0.0f,1.0f,0.157f,0.842f);
                            break;
                    }
                    break;
                case "FILE":
                    String fileName = "";
                    for(int i=1;i<dataParts.length;i++)
                        fileName+=dataParts[i];
                    FILE_URL = Environment.getExternalStorageDirectory().toString()+fileName;
                    init();
            }

        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(c!=null)
        {
            c.startReceiving();
        }
        else
        {
            c = new Client(serverIp,this);
            c.startReceiving();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(c!=null)
        {
            c.stopReceiving();
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
    {
        Surface surface = new Surface(surfaceTexture);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer
                    .setDataSource(FILE_URL);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer)
                {
                    mediaPrepared = true;
                    Log.d(TAG," mediaPrepared");
                }
            });
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mMediaPlayer != null)
        {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void setTextureView(int orientation,float values[])
    {
        setTextureView(orientation,values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7]);
    }

    private void setTextureView(int orientation,
                                float x1,float x2,float y1,float y2,
                                float offsetX1,float offsetX2,
                                float offsetY1,float offsetY2)
    {
        int DEVHEIGHT,DEVWIDTH;
        if(orientation==1)
        {
            DEVHEIGHT = dev.devHeight;
            DEVWIDTH = dev.devWidth;
        }
        else
        {
            DEVWIDTH = dev.devHeight;
            DEVHEIGHT= dev.devWidth;
        }
        int heightDisp = (int)((y2-y1)*dev.vidHeight);
        int widthDisp = (int)((x2-x1)*dev.vidWidth);
        int heightScaled = (int)((offsetY2-offsetY1)*DEVHEIGHT);
        int widthScaled = (int)((offsetX2-offsetX1)*DEVWIDTH);
        int h1 = (int)(offsetY1*DEVHEIGHT);
        int h2 = (int)((1.0-offsetY2)*DEVHEIGHT);
        int w1 = (int)(offsetX1*DEVWIDTH);
        int w2 = (int)((1.0-offsetX2)*DEVWIDTH);
        Matrix matrix = new Matrix();

        float scaleH = (float)dev.vidHeight/(float)heightDisp;
        float scaleW = ((float)heightScaled/(float)heightDisp)/((float)(DEVWIDTH)/(float)dev.vidWidth);
        float scale = (float)heightScaled/(float)heightDisp;
        float x = -x1*dev.vidWidth*scale+w1;
        float y = -y1*dev.vidHeight*scale+h1;

        matrix.setScale(scaleW,scaleH,0,0);
        matrix.postTranslate(x,y);

        mTextureView.setTransform(matrix);
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(DEVWIDTH-w2,DEVHEIGHT-h2));

    }

    private void init()
    {
        dev = new DeviceModel();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dev.devHeight = metrics.heightPixels;
        dev.devWidth  = metrics.widthPixels;
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(FILE_URL);
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        dev.vidHeight = Integer.parseInt(height);
        dev.vidWidth = Integer.parseInt(width);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(this);
    }

    public void requestServer()
    {
        c = new Client(serverIp,this);
        c.connectToServer();
        c.startReceiving();
    }
}
