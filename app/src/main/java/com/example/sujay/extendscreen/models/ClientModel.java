package com.example.sujay.extendscreen.models;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Created by sujay on 8/8/15.
 */
public class ClientModel
{
    public final InetAddress clientAddress;
    public final int port;
    private int msgNo;
    public final int devNo;
    private int tryNo;
    private Rect rectangle = null;
    public ClientModel(InetAddress clientAddress,int port,int devNo)
    {
        this.clientAddress = clientAddress;
        this.port = port;
        this.devNo = devNo;
        this.setMsgNo(1);
        this.setTryNo(0);
    }
    public synchronized void setMsgNo(int msgNo)
    {
        this.msgNo = msgNo;
    }
    public synchronized int getMsgNo()
    {
        return this.msgNo;
    }
    public synchronized void setTryNo(int tryNo)
    {
        this.tryNo = tryNo;
    }
    public synchronized int getTryNo()
    {
        return this.tryNo;
    }
    public Rect getRectangle()
    {
        return rectangle;
    }
    public void setRectangle(Rect r)
    {
        rectangle = r;
    }

}
