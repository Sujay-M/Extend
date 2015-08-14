package com.example.sujay.extendscreen.utils;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created by sujay on 8/8/15.
 */
public class Client implements ClientReceiverTask.ClientMessageReceived
{

    public interface CommandFromServer
    {
        void commandReceived(String type,String data);
    }
    private static final String TAG = "CLIENT";
    final static int port = 9999;
    InetAddress serverAddress;
    int msgNo,devNo;
    CommandFromServer callback;
    ClientReceiverTask receiver;
    DatagramSocket receiverSocket = null;
    public Client(InetAddress serverAddress,CommandFromServer callback)
    {
        this.serverAddress = serverAddress;
        this.callback = callback;
        this.msgNo = 0;
        setDevNo(-1);
    }
    public DatagramPacket buildAck()
    {
        String s = new String(devNo+" "+msgNo+" "+"ACK");
        byte buf[];
        buf = s.getBytes();
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, serverAddress, port);
        return pkt;
    }
    public void sendData(DatagramPacket sendPkt)
    {
        try {
            DatagramSocket sender = new DatagramSocket();
            sender.send(sendPkt);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized int getDevNo()
    {
        return devNo;
    }
    public synchronized void setDevNo(int devNo)
    {
        this.devNo = devNo;
    }
    public void connectToServer()
    {
        final DatagramPacket ack = buildAck();
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(getDevNo()==-1)
                {
                    sendData(ack);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG,"devNo = "+getDevNo());
            }
        });
        t.start();
    }
    @Override
    public void msgReceived(String msg)
    {
        final int messageNumber;
        startReceiving();
        if(msg!=null)
        {
            String msgParts[] = msg.split(" ");
            messageNumber = Integer.parseInt(msgParts[0]);
            Log.d(TAG,"message received no = "+messageNumber+" current = "+msgNo);
            if(messageNumber==(msgNo+1) || messageNumber==msgNo)
            {

                msgNo = messageNumber;
                Log.d(TAG,"sending message no :"+msgNo);
                new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        DatagramPacket pkt = buildAck();
                        sendData(pkt);
                    }
                } ).start();
            }
            if(messageNumber==1 && msgParts[2].equals("DEVNO"))
            {
                this.setDevNo(Integer.parseInt(msgParts[1]));
            }
            callback.commandReceived(msgParts[1],msg.substring(msgParts[0].length()+msgParts[1].length()+2));
        }

    }
    public void startReceiving()
    {
        if(receiverSocket == null)
        {
            try {
                receiverSocket = new DatagramSocket(null);
                receiverSocket.setReuseAddress(true);
                receiverSocket.bind(new InetSocketAddress(port));
                receiver = new ClientReceiverTask(receiverSocket,serverAddress,this);
                receiver.execute();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        else
        {
            receiver = new ClientReceiverTask(receiverSocket,serverAddress,this);
            receiver.execute();
        }

    }
    public void stopReceiving()
    {
        receiverSocket.close();
        receiverSocket = null;
        if(receiver!=null && !receiver.isCancelled())
            receiver.cancel(true);
    }
    public void caliberateTime()
    {
        long timeSent = SystemClock.elapsedRealtime();
        byte buf[];
        String s = new String(-1+" "+0+" "+"ACK "+timeSent);
        buf = s.getBytes();
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, serverAddress, port);
    }
}
