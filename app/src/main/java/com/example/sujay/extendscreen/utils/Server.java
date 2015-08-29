package com.example.sujay.extendscreen.utils;

import android.os.SystemClock;
import android.util.Log;

import com.example.sujay.extendscreen.models.ClientModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sujay on 8/8/15.
 */
public class Server implements ServerReceiverTask.ServerMessageReceived
{
    boolean acceptClients;
    ArrayList<ClientModel> clients;
    HashMap<InetAddress,Integer> addressHashMap;
    private static Server INSTANCE = null;
    final static int port = 9999;
    private static final int MAXTRY = 10;
    DatagramSocket receiverSocket;
    ServerReceiverTask receiver;
    private Server()
    {
        clients = new ArrayList<ClientModel>();
        addressHashMap = new HashMap<InetAddress,Integer>();
        addressHashMap.clear();
        acceptClients = true;
    }
    public static synchronized Server getSingleton()
    {
        /**return the singleton*/
        if(INSTANCE==null)
        {
            INSTANCE = new Server();
        }
        return INSTANCE;
    }
    public void startServer()
    {
        /**start accepting the packets*/
        if(receiverSocket == null)
        {
            try {
                receiverSocket = new DatagramSocket(null);
                receiverSocket.setReuseAddress(true);
                receiverSocket.bind(new InetSocketAddress(port));

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        receiver = new ServerReceiverTask(receiverSocket,this);
        receiver.execute();


    }
    public void stopServer()
    {
        receiverSocket.close();
        if(receiver!=null && !receiver.isCancelled())
            receiver.cancel(true);
        receiver = null;
    }

    @Override
    public void msgReceived(DatagramPacket msg)
    {
        /** When the message from the client arrives it is either the first packet or the acknowledgement
         * packet. If it is the first packet then send a packet that tells the client that server has recognized it and
         * send its device no.
         * if the packet is an acknowledgement packet then increment the current message number to stop resending the
         * same packet.
         */
        if(msg==null)
            Log.d("server msg","msg is null");
        Log.d("server msg","msg received");
        startServer();
        InetAddress clientAddr = msg.getAddress();
        int clientPort = 9999;
        String message = new String(msg.getData(), 0, msg.getLength());
        String msgParts[] = message.split(" ");
        Log.d("server","message received"+addressHashMap.containsKey(clientAddr));
        if(msgParts.length==3||msgParts.length==4)
        {
            if(acceptClients && !addressHashMap.containsKey(clientAddr) && msgParts[0].equals("-1") && msgParts[1].equals("0") && msgParts[2].equals("ACK"))
            {
                ClientModel temp = new ClientModel(clientAddr,clientPort,clients.size());
                clients.add(temp);
                addressHashMap.put(clientAddr,temp.devNo);
                Log.d("server","adding addr "+addressHashMap.containsKey(clientAddr));
                DatagramPacket pkt = buildPacket(temp.devNo,temp.devNo+" DEVNO");
                sendToClient(temp.devNo,pkt);
            }
            else if(addressHashMap.containsKey(clientAddr) && (!msgParts[0].equals("-1")))//check if the inetaddress is in clients
            {
                int devNo = Integer.parseInt(msgParts[0]);
                int messageNo = Integer.parseInt(msgParts[1]);
                ClientModel temp = clients.get(devNo);
                if(msgParts[2].equals("ACK"))
                {
                    Log.d("ACK","message no = "+messageNo);
                    temp.setMsgNo(messageNo+1);
                }
                if(msgParts[2].equals("SYNC"))
                {
                    Log.d("SERVER","SYNC request");
                    try
                    {
                        long clientTime = Long.parseLong(msgParts[3]);
                        long serverTime = SystemClock.elapsedRealtime();
                        long skew = clientTime-serverTime;
                        DatagramPacket sync = buildPacket(devNo,"DATA SYNC "+skew);
                        Log.d("SERVER","client "+devNo+" Skew found "+skew);
                        sendToClient(devNo,sync);
                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {

                    }

                }


            }


        }
    }
    public  void sendToClient(int devNo,final DatagramPacket sendPkt)
    {
        /**Send the client whose device number devNo the packet.
         * Keep sending the packet till the message number increments that is we receive an
         * ack packet from the concerned client.
         */
        try
        {
            final ClientModel temp = clients.get(devNo);
            Thread t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    int currentNo = temp.getMsgNo();
                    Log.d("sending ","msg no = "+currentNo);
                    while(temp.getMsgNo()==currentNo && temp.getTryNo()<MAXTRY)
                    {
                        int tryNo = temp.getTryNo();
                        temp.setTryNo(tryNo+1);
                        DatagramSocket sender = null;
                        try
                        {
                            sender = new DatagramSocket();
                            sender.send(sendPkt);
                            Thread.sleep(1000);
                        } catch (SocketException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    if(temp.getTryNo()>MAXTRY)
                        Log.d("ack not received ","msg no = "+currentNo+" msg no = "+temp.getMsgNo());
                    else
                        Log.d("ack received ","msg no = "+currentNo+" msg no = "+temp.getMsgNo());
                    temp.setTryNo(0);
                }
            });
            t.start();
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
        }
    }
    public DatagramPacket buildPacket(int devNo,String msg)
    {
        /**construct the packet*/
        DatagramPacket pkt;
        byte buf[];
        ClientModel temp = clients.get(devNo);
        buf = new String(temp.getMsgNo()+" "+msg).getBytes();
        pkt = new DatagramPacket(buf,buf.length,temp.clientAddress,temp.port);
        return pkt;
    }
    public void sendToAll(String msg)
    {
        if(msg.contains("PLAY")||msg.contains("PAUSE")||msg.contains("SEEK"))
        {
                msg = msg+" "+SystemClock.elapsedRealtime();
        }
        for (int i = 0;i<clients.size();i++)
        {
            DatagramPacket pkt = buildPacket(i,msg);
            sendToClient(i,pkt);
        }
    }
    public int getNoOfClients()
    {
        if(clients!=null)
            return clients.size();
        return 0;
    }
    public ClientModel getClient(int num)
    {
        if(num<clients.size())
            return clients.get(num);
        return null;
    }


}
