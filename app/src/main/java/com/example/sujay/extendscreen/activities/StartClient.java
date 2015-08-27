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
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.models.DeviceModel;
import com.example.sujay.extendscreen.utils.Client;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by sujay on 5/8/15.
 */
public class StartClient extends Activity implements Client.CommandFromServer, TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener {
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
    Surface surface;
    long clock_skew=0;
    int sync_no;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.client_activity);
        mainView = (FrameLayout)findViewById(R.id.flMainView);
        tvMessageReceived = (TextView)findViewById(R.id.tvMessageReceived);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(this);
        serverIp = (InetAddress) getIntent().getExtras().get("ServerIP");
        requestServer();
    }


    @Override
    public void commandReceived(String type, String data)
    {
        tvMessageReceived.setText("TYPE: "+type+"  DATA:"+data);
        String dataParts[] = data.split(" ");

        if(type.equals("COMMAND"))
        {
            long serverTime,clientTime,sleepTime;
            switch(dataParts[0])
            {
                case "WHITE":
                    mainView.setBackgroundColor(Color.WHITE);
                    break;
                case "RED":
                    mainView.setBackgroundColor(Color.RED);
                    break;
                case "BLACK":
                    mainView.setBackgroundColor(Color.BLACK);
                    break;
                case "PLAY":
                    Log.d(TAG,"play");
                    if(mediaPrepared)
                    {
                        serverTime = Long.parseLong(dataParts[1])+1000;
                        clientTime = SystemClock.elapsedRealtime();
                        sleepTime = (serverTime+clock_skew) - clientTime;
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mMediaPlayer.start();
                    }
                    break;
                case "PAUSE":
                    if(mediaPrepared)
                    {
                        serverTime = Long.parseLong(dataParts[1])+1000;
                        clientTime = SystemClock.elapsedRealtime();
                        sleepTime = (serverTime+clock_skew) - clientTime;
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mMediaPlayer.pause();
                    }
                    break;
                case "STOP":
                    if(mediaPrepared)
                    {
                        stopPlayer();
                    }
                    break;
                case "SEEK":
                    if(mediaPrepared)
                    {
                        serverTime = Long.parseLong(dataParts[1])+1000;
                        clientTime = SystemClock.elapsedRealtime();
                        sleepTime = (serverTime+clock_skew) - clientTime;
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case "PREPARE":
                    mMediaPlayer.prepareAsync();
                    break;
                case "SYNC":
                    initiateSynchronization();
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
                        case "0":
                            setTextureView(1,0.0f,0.5f,0.0f,1.0f,0.0f,1.0f,0.157f,0.842f);
                            break;
                        case "1":
                            setTextureView(1,0.5f,1.0f,0.0f,1.0f,0.0f,1.0f,0.157f,0.842f);
                            break;
                    }
                    break;
                case "FILE":
                    String fileName = ""+dataParts[1];
                    for(int i=2;i<dataParts.length;i++)
                        fileName+=" "+dataParts[i];
                    FILE_URL = Environment.getExternalStorageDirectory().toString()+fileName;
                    Log.d(TAG,FILE_URL);
                    if(new File(FILE_URL).exists())
                    {
                        Log.d(TAG,"File exists");
                        init();
                    }
                    break;
                case "SYNC":
                    if(clock_skew==0)
                    {
                        sync_no = 1;
                        clock_skew = Long.parseLong(dataParts[1]);
                        Log.d(TAG,"Initial clock skew = "+clock_skew);
                    }
                    else
                    {
                        clock_skew = (clock_skew*sync_no+Long.parseLong(dataParts[1]))/(sync_no+1);
                        sync_no++;
                        Log.d(TAG,"obtained skew = "+dataParts[1]+" current = "+clock_skew);
                    }
                    break;

            }

        }

    }

    private void initiateSynchronization()
    {
        sync_no = 0;
         Thread t = new Thread(new Runnable() {
             @Override
             public void run()
             {
                for (int i=1;i<=10;i++)
                {
                    DatagramPacket sync = c.buildSyncPacket();
                    c.sendData(sync);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
             }
         });
        t.start();
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
        surface = new Surface(surfaceTexture);
        initMediaPlayer();

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
        try
        {
            metaRetriever.setDataSource(FILE_URL);
            String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            dev.vidHeight = Integer.parseInt(height);
            dev.vidWidth = Integer.parseInt(width);
        }catch(IllegalArgumentException e)
        {
            Toast.makeText(this,"File cant be opened",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        try
        {
            stopPlayer();
            initMediaPlayer();
            mMediaPlayer.setDataSource(FILE_URL);
            mMediaPlayer.prepareAsync();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        metaRetriever.release();
        metaRetriever = null;
    }

    private void stopPlayer()
    {
        if(mMediaPlayer!=null)
        {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mediaPrepared = false;
        }
    }

    private void initMediaPlayer()
    {
        try
        {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnPreparedListener(this);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void requestServer()
    {
        c = new Client(serverIp,this);
        c.connectToServer();
        c.startReceiving();
    }
    @Override
    public void onPrepared(MediaPlayer mediaPlayer)
    {
        mediaPrepared = true;
        Log.d(TAG," mediaPrepared");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
