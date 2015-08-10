package com.example.sujay.extendscreen.utils;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by sujay on 9/8/15.
 */
public class ServerReceiverTask extends AsyncTask<Void,Void,DatagramPacket>
{
    interface ServerMessageReceived
    {
        void msgReceived(DatagramPacket msg);
    }
    final static int port = 9999;
    private ServerMessageReceived callback;
    DatagramSocket receiver;
    public ServerReceiverTask(DatagramSocket receiver,ServerMessageReceived callback)
    {
        this.callback = callback;
        this.receiver = receiver;
    }
    @Override
    protected DatagramPacket doInBackground(Void... params)
    {

        try
        {
            byte[] buf = new byte[1024];
            DatagramPacket message = new DatagramPacket(buf, 1024);
            receiver.receive(message);
            return message;
        }catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(DatagramPacket s) {
        super.onPostExecute(s);
        callback.msgReceived(s);
    }
}
