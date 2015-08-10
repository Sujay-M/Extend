package com.example.sujay.extendscreen.utils;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by sujay on 5/8/15.
 */
public class ClientReceiverTask extends AsyncTask<Void,Void,String>
{
    interface ClientMessageReceived
    {
        void msgReceived(String msg);
    }
    private ClientMessageReceived callback;
    InetAddress serverAddress;
    final static int port = 9999;
    DatagramSocket receiver;
    public ClientReceiverTask(DatagramSocket receiver,InetAddress serverAddress, ClientMessageReceived callback)
    {
        this.serverAddress = serverAddress;
        this.callback = callback;
        this.receiver = receiver;
    }
    @Override
    protected String doInBackground(Void... params)
    {
        try
        {
            byte[] buf = new byte[1024];
            DatagramPacket message = new DatagramPacket(buf, 1024);
            do
            {
                receiver.receive(message);
            }while (!(message.getAddress().equals(serverAddress)));
            String str = new String(message.getData(), 0, message.getLength());
            return str;
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s)
    {
        super.onPostExecute(s);
        callback.msgReceived(s);
    }
}
