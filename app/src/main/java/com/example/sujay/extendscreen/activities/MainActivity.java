package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.sujay.extendscreen.R;


public class MainActivity extends Activity implements View.OnClickListener
{
    private final static String TAG = "MainActivity";

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bClient).setOnClickListener(this);
        findViewById(R.id.bServer).setOnClickListener(this);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ((TextView)findViewById(R.id.tvHeight)).setText(""+metrics.heightPixels);
        ((TextView)findViewById(R.id.tvWidth)).setText(""+metrics.widthPixels);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onClick(View v)
    {
        Intent i;

        switch(v.getId())
        {
            case R.id.bServer:
                i = new Intent(getBaseContext(),StartServer.class);
                i.putExtra("IS_MASTER",false);
                Log.d(TAG, "server start");
                startActivity(i);
                break;
            case R.id.bClient:
                i = new Intent(getBaseContext(),StartClient.class);
                i.putExtra("IS_MASTER",true);
                Log.d(TAG, "client start");
                startActivity(i);
                break;
        }
    }
}
