package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.utils.Server;

import java.net.DatagramPacket;

/**
 * Created by sujay on 5/8/15.
 */
public class StartServer extends Activity implements View.OnClickListener
{
    Server server;
    EditText etDno,etType,etData;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_activity);
        etDno = (EditText)findViewById(R.id.etDno);
        etType = (EditText)findViewById(R.id.etType);
        etData = (EditText)findViewById(R.id.etData);
        findViewById(R.id.bSend).setOnClickListener(this);
        Intent i = new Intent(this, DispServerDetails.class);
        startActivity(i);
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
        int dno = Integer.parseInt(etDno.getText().toString());
        DatagramPacket pkt = server.buildPacket(dno ,
                etType.getText().toString()+" "+etData.getText().toString());
        Log.d("send button pkt = ",pkt.toString());
        server.sendToClient(dno,pkt);
    }
}
