package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.sujay.extendscreen.R;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by sujay on 9/8/15.
 */
public class GetServerDetails extends Activity
{
    EditText etIP,etPort;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_server_details);
        etIP = (EditText)findViewById(R.id.etServerIP);
        findViewById(R.id.bDone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                try {
                    InetAddress sIP = InetAddress.getByName(etIP.getText().toString());
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("ServerIP",sIP);
                    setResult(RESULT_OK,returnIntent);
                    finish();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
            }
        });
    }
}
