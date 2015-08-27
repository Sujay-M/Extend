package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.utils.Server;

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
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        server = Server.getSingleton();
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

                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }
}
