package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.sujay.extendscreen.R;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by sujay on 9/8/15.
 */
public class GetServerDetails extends Activity implements View.OnClickListener
{
    EditText etIP,etPort;
    private final static String TAG = "GetServerDetails";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_server_details);
        etIP = (EditText)findViewById(R.id.etServerIP);
        findViewById(R.id.bDone).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        try
        {
            if(etIP.getText().toString().equals(""))
            {
                Toast.makeText(this,"Please Input the correct IP",Toast.LENGTH_LONG).show();
                return;
            }
            final Intent i = new Intent(this,StartClient.class);
            InetAddress sIP = InetAddress.getByName(etIP.getText().toString());
            i.putExtra("ServerIP",sIP);
            startActivity(i);
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
            Toast.makeText(this,"Please Input the correct IP",Toast.LENGTH_LONG).show();
        }
        catch (NetworkOnMainThreadException e)
        {
            e.printStackTrace();
            Toast.makeText(this,"Please Input the correct IP",Toast.LENGTH_LONG).show();
        }
    }
}
