package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.utils.Client;

import java.net.InetAddress;

/**
 * Created by sujay on 5/8/15.
 */
public class StartClient extends Activity implements Client.CommandFromServer {
    Client c = null;
    TextView tvMessageReceived;
    InetAddress serverIp;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_activity);
        Intent i = new Intent(this, GetServerDetails.class);
        tvMessageReceived = (TextView)findViewById(R.id.tvMessageReceived);
        startActivityForResult(i, 1);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1)
        {
            if(resultCode == RESULT_OK)
            {
                serverIp = (InetAddress) data.getExtras().get("ServerIP");
                requestServer();
            }
            if (resultCode == RESULT_CANCELED)
            {
                //Write your code if there's no result
            }
        }
    }
    public void requestServer()
    {
        c = new Client(serverIp,this);
        c.connectToServer();
        c.startReceiving();
    }

    @Override
    public void commandReceived(String type, String data)
    {
        tvMessageReceived.setText("TYPE: "+type+"  DATA:"+data);
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
    protected void onPause() {
        super.onPause();
        if(c!=null)
        {
            c.stopReceiving();
        }
    }
}
