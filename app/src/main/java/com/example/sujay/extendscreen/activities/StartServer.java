package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.sujay.extendscreen.ImageProcessing.LayoutModel;
import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.models.ClientModel;
import com.example.sujay.extendscreen.utils.Server;

import org.opencv.core.Rect;

import java.io.File;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sujay on 5/8/15.
 */
public class StartServer extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    Server server;
    private final static String TAG = "StartServer";
    private boolean playing = false;
    Button bPlay,bCaliberate;
    Spinner sType,sFiles;
    List<String> type;
    List<String> files;
    List <Integer[]> layout;
    String selectedFile,selectedFolder;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_activity);
        findViewById(R.id.bSynchronize).setOnClickListener(this);
        bPlay = (Button)findViewById(R.id.bPlay);
        bCaliberate = (Button)findViewById(R.id.bCaliberate);
        bPlay.setOnClickListener(this);
        bCaliberate.setOnClickListener(this);
        sType = (Spinner)findViewById(R.id.sType);
        sFiles = (Spinner)findViewById(R.id.sFile);
        type = new ArrayList<String>();
        files = new ArrayList<String>();
        type.add("Videos");
        type.add("Images");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,type);
        sType.setAdapter(typeAdapter);
        sType.setOnItemSelectedListener(this);
        layout = LayoutModel.getSingleton().getLayout();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        server = Server.getSingleton();
        server.sendToAll("COMMAND BLACK");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.bPlay:
                if(playing)
                {

                    server.sendToAll("COMMAND PAUSE");
                    playing = false;
                    bPlay.setText("PLAY");
                }
                else
                {
                    server.sendToAll("COMMAND PLAY");
                    playing = true;
                    bPlay.setText("PAUSE");
                }
                break;
            case R.id.bCaliberate:
                bCaliberate.setText("RECALIBERATE");
                for (int i=0;(i<server.getNoOfClients()&&i<2);i++)
                {
                    DatagramPacket pkt = server.buildPacket(i,"DATA CALIB "+i);
                    server.sendToClient(i,pkt);
                }
                break;
            case R.id.bSynchronize:
                server.sendToAll("COMMAND SYNC");
                break;
        }

    }
    private void calibrate(int width, int height)
    {
        if(layout!=null)
        {
            int max = 0,selHeight = 0,selWidth = 0;
            Integer[] selected = null;
            for(Integer[] rect:layout)
            {
                int newHeight,newWidth;
                int w = rect[2] - rect[0];
                int h = rect[3] - rect[1];
                float heightScale = ((float)h/(float)height);
                float widthScale = ((float)w/(float)width);
                if(heightScale>=widthScale)
                {
                   newHeight = (int)(height*widthScale);
                   newWidth = w;
                }
                else
                {
                    newWidth = (int)(width*heightScale);
                    newHeight = h;
                }
                Log.d(TAG,"dimens video = ("+width+","+height+")");
                Log.d(TAG,"dimens imageProcessing = ("+w+","+h+")");
                Log.d(TAG,"dimens scaled = ("+newWidth+","+newHeight+")");
                int area = newHeight*newWidth;
                if(area>max)
                {
                    max = area;
                    selected = rect;
                    selHeight = newHeight;
                    selWidth = newWidth;
                }
            }
            if(selected!=null)
            {
                int offH = (int)((float)((selected[3]-selected[1])-selHeight)/2);
                int offW = ((selected[2]-selected[0])-selWidth)/2;

                int left = selected[0]+offW;
                int right = selected[2]-offW;
                int top = selected[1]+offH;
                int bottom = selected[3]-offH;
                Log.d(TAG,"SELECTED left = "+left+" right = "+right+" top = "+top+" bottom = "+bottom);
                for(int i = 0;i < server.getNoOfClients();i++)
                {
                    ClientModel c = server.getClient(i);
                    Rect r = c.getRectangle();
                    int orientation = 1;
                    int l = (r.x>left)?r.x:left;
                    int ri = (r.x+r.width<right)?r.x+r.width:right;
                    int t = (r.y>top)?r.y:top;
                    int b = (r.y+r.height<bottom?r.y+r.height:bottom);
                    Log.d(TAG,"client actual l = "+r.x+" r = "+(r.x+r.width)+" t = "+r.y+" b = "+(r.y+r.height));
                    Log.d(TAG,"client left = "+l+" right = "+ri+" top = "+t+" bottom = "+b);
                    float x1 = getPercentage(l-left,right-left);
                    float x2 = getPercentage(ri-left,right-left);
                    float y1 = getPercentage(t-top,bottom-top);
                    float y2 = getPercentage(b-top,bottom-top);
                    float offx1 = getPercentage(l-r.x,r.width);
                    float offx2 = getPercentage(ri-r.x,r.width);
                    float offy1 = getPercentage(t-r.y,r.height);
                    float offy2 = getPercentage(b-r.y,r.height);
                    String msg = "DATA CALIB "+ orientation +" "+x1 +" "+x2 +" "+y1 +" "+y2 +" "+offx1 +" "+offx2 +" "+offy1 +" "+offy2;
//                    String msg = "DATA CALIB 0 0.0 1.0 0.0 1.0 0.0 1.0 0.2 0.8";
                    DatagramPacket pkt = server.buildPacket(i,msg);
                    server.sendToClient(i,pkt);
                }
            }
        }
        else
            Toast.makeText(this,"Layout not Selected",Toast.LENGTH_LONG).show();
    }
    private float getPercentage(int val,int total)
    {
        return (float)val/(float)total;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Log.d(TAG,"item Selected");
        switch(parent.getId())
        {
            case R.id.sType:

                selectedFolder = "/Extend/"+parent.getItemAtPosition(position).toString();

                String path = Environment.getExternalStorageDirectory().toString()+selectedFolder;
                Log.d(TAG, "Path: " + path);
                files.clear();
                File f = new File(path);
                File file[] = f.listFiles();
                Log.d(TAG, "no of files: "+ file.length);
                for (int i=0; i < file.length; i++)
                {
                    Log.d(TAG, "FileName:" + file[i].getName());
                    files.add(file[i].getName());
                }
                ArrayAdapter<String> fileAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,files);
                sFiles.setAdapter(fileAdapter);
                sFiles.setOnItemSelectedListener(this);

                break;
            case R.id.sFile:
                selectedFile = parent.getItemAtPosition(position).toString();
                if(playing)
                {
                    server.sendToAll("COMMAND STOP");
                    bPlay.setText("PLAY");
                    playing = false;
                }
                server.sendToAll("DATA FILE "+selectedFolder+"/"+selectedFile);
                if(selectedFolder.contains("Videos"))
                {
                    int videoHeight,videoWidth;
                    try
                    {
                        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                        path = Environment.getExternalStorageDirectory().toString()+selectedFolder;
                        metaRetriever.setDataSource(path+"/"+selectedFile);
                        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                        videoHeight = Integer.parseInt(height);
                        videoWidth = Integer.parseInt(width);
                        calibrate(videoWidth, videoHeight);
                        metaRetriever = null;
                    }catch(IllegalArgumentException e)
                    {
                        Toast.makeText(this,"File cant be opened",Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

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
