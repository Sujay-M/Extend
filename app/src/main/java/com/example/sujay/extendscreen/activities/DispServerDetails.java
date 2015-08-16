package com.example.sujay.extendscreen.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.sujay.extendscreen.R;
import com.example.sujay.extendscreen.utils.Server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by sujay on 9/8/15.
 */
public class DispServerDetails extends Activity implements View.OnClickListener {
    Server s;
    private final static String TAG = "DispServerDetails";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.disp_server_details);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        String ip = "";
        if (mWifi.isConnected())
        {

            WifiInfo info =  wifiManager.getConnectionInfo();
            ip = getWifiIpAddress(info);
        }
        else
        {
            Method method = null;
            try
            {
                method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
                int actualState = (Integer) method.invoke(wifiManager, (Object[]) null);
                method.setAccessible(true);
                if(actualState==13)
                {
                    String mac = wifiManager.getConnectionInfo().getMacAddress();
                    if(mac!=null)
                    {
                        ip = getIpAddress(getMac(mac));
                    }
                }

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }


        ((TextView)findViewById(R.id.tvServerIP)).setText(ip);
        s = Server.getSingleton();
        s.startServer();
        findViewById(R.id.bNext).setOnClickListener(this);

    }
    protected String getWifiIpAddress(WifiInfo info)
    {
        int ipAddress = info.getIpAddress();
        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Unable to get host address.");
            ipAddressString = null;
        }
        return ipAddressString;
    }

    private String getIpAddress(final byte[] macBytes)
    {
        String ip = null;

        try
        {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                if(Arrays.equals(networkInterface.getHardwareAddress(), macBytes))
                {
                    Enumeration<InetAddress> enumInetAddress = networkInterface
                            .getInetAddresses();
                    while (enumInetAddress.hasMoreElements())
                    {
                        InetAddress inetAddress = enumInetAddress.nextElement();
                        if (inetAddress.isSiteLocalAddress())
                        {
                            return inetAddress.getHostAddress();
                        }

                    }
                }


            }

        }
        catch (SocketException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip = null;
        }

        return ip;
    }
    private  byte[] getMac(String mac)
    {
        final String[] macParts = mac.split(":");
        final byte[] macBytes = new byte[macParts.length];
        for(int i = 0;i<macParts.length;i++)
        {
            macBytes[i] = (byte)Integer.parseInt(macParts[i],16);
        }
        return macBytes;
    }

    @Override
    public void onClick(View v)
    {
        Intent i = new Intent(this,StartServer.class);
        startActivity(i);
    }
}
