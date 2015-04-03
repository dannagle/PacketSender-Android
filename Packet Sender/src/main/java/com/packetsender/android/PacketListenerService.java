//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Set;


public class PacketListenerService extends IntentService {

    private NotificationManager mNM;
    private final int NOTIFICATIONID = 100;
    private int listenportTCP = 5000;
    private int listenportUDP = 5000;
    private Notification updateComplete;
    private int packetCounter = 0;
    private PendingIntent contentIntent;
    private Runnable shutdownListener;
    private Runnable sendListener;

    private ServerSocketChannel tcpserver;
    private DatagramChannel udpserver;
    private final Handler mHandler = new Handler();

    private DataStorage dataStore;

    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";

    public PacketListenerService() {
        super("PacketListenerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        dataStore = new DataStorage(
                getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
        );


        listenportTCP = dataStore.getTCPPort();
        listenportUDP = dataStore.getUDPPort();
        Log.i("service", DataStorage.FILE_LINE("TCP: " + listenportTCP + " / UDP: " + listenportUDP));


        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);


        startNotification ();

        CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();
        ByteBuffer response = null;
        try {
            response = encoder.encode(CharBuffer.wrap("response"));
        } catch (CharacterCodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {

            SocketAddress localportTCP = new InetSocketAddress(listenportTCP);
            SocketAddress localportUDP = new InetSocketAddress(listenportUDP);

            tcpserver = ServerSocketChannel.open();
            tcpserver.socket().bind(localportTCP);

            udpserver = DatagramChannel.open();
            udpserver.socket().bind(localportUDP);

            tcpserver.configureBlocking(false);
            udpserver.configureBlocking(false);

            Selector selector = Selector.open();


            tcpserver.register(selector, SelectionKey.OP_ACCEPT);
            udpserver.register(selector, SelectionKey.OP_READ);


            ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
            receiveBuffer.clear();


            shutdownListener = new Runnable() {
                public void run() {

                    if(false)
                    {

                        try {
                            tcpserver.close();
                        } catch (IOException e) {
                        }
                        try {
                            udpserver.close();
                        } catch (IOException e) {
                        }
                        stopSelf();
                    } else {
                        mHandler.postDelayed(shutdownListener,2000);

                    }

                }
            };

            sendListener = new Runnable() {
                public void run() {

                    //Packet fetchedPacket = mDbHelper.needSendPacket();
                    Packet [] fetchedPackets = dataStore.fetchAllServicePackets();

                    if(fetchedPackets.length > 0) {
                        dataStore.clearServicePackets();
                        Log.d("service", DataStorage.FILE_LINE("sendListener found " + fetchedPackets.length + " packets"));

                        for(int i = 0; i < fetchedPackets.length; i++)
                        {
                            Packet fetchedPacket = fetchedPackets[i];
                            Log.d("service", DataStorage.FILE_LINE("send packet " + fetchedPacket.toString()));

                        }

                        new SendPacketsTask().execute(fetchedPackets);
                    }



                    mHandler.postDelayed(sendListener,2000);

                }
            };


            //start shutdown listener
            mHandler.postDelayed(shutdownListener,2000);

            //start send listener
            mHandler.postDelayed(sendListener,5000);

            while (true) {
                try { // Handle per-connection problems below
                    // Wait for a client to connect
                    Log.d("service", DataStorage.FILE_LINE("waiting for connection"));
                    selector.select();
                    Log.d("service", DataStorage.FILE_LINE("client connection"));



                    Set keys = selector.selectedKeys();


                    for (Iterator i = keys.iterator(); i.hasNext();) {

                        SelectionKey key = (SelectionKey) i.next();
                        i.remove();


                        Channel c = (Channel) key.channel();


                        if (key.isAcceptable() && c == tcpserver) {

                            SocketChannel client = tcpserver.accept();

                            if (client != null)
                            {

                                Socket tcpSocket = client.socket();
                                packetCounter++;


                                DataInputStream  in = new DataInputStream (
                                        tcpSocket.getInputStream());


                                byte[] buffer = new byte[1024];
                                int received = in.read(buffer);
                                byte[] bufferConvert = new byte[received];
                                System.arraycopy(buffer, 0, bufferConvert, 0, bufferConvert.length);


                                Packet storepacket = new Packet();
                                storepacket.tcpOrUdp = "TCP";
                                storepacket.fromIP = tcpSocket.getInetAddress().getHostAddress();

                                storepacket.toIP = "You";
                                storepacket.fromPort = tcpSocket.getPort();
                                storepacket.port = tcpSocket.getLocalPort();
                                storepacket.data = bufferConvert;

                                UpdateNotification("TCP:" + storepacket.toAscii(), "From " + storepacket.fromIP);

                                Log.i("service", DataStorage.FILE_LINE("Got TCP"));
                                //dataStore.SavePacket(storepacket);

                                /*
                                Intent tcpIntent = new Intent();
                                tcpIntent.setAction(ResponseReceiver.ACTION_RESP);
                                tcpIntent.addCategory(Intent.CATEGORY_DEFAULT);
                                tcpIntent.putExtra(PARAM_OUT_MSG, storepacket.name);
                                sendBroadcast(tcpIntent);
                                */

                                storepacket.nowMe();
                                dataStore.saveTrafficPacket(storepacket);
                                Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));


                                if(false ) //mDbHelper.getSettings(PSDbAdapter.KEY_SETTINGS_SENDRESPONSE).equalsIgnoreCase("Yes"))
                                {
                                    storepacket = new Packet();
                                    storepacket.name = dataStore.currentTimeStamp();;
                                    storepacket.tcpOrUdp = "TCP";
                                    storepacket.fromIP = "You";
                                    storepacket.toIP = tcpSocket.getInetAddress().getHostAddress();
                                    storepacket.fromPort = tcpSocket.getLocalPort();
                                    storepacket.port = tcpSocket.getPort();
                                   // storepacket.data = Packet.toBytes(mDbHelper.getSettings(PSDbAdapter.KEY_SETTINGS_SENDRESPONSETEXT));


                                    storepacket.nowMe();
                                    dataStore.saveTrafficPacket(storepacket);
                                    Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));

                                    client.write(response); // send response
                                }


                                client.close(); // close connection
                            }
                        } else if (key.isReadable() && c == udpserver) {

                            DatagramSocket udpSocket;
                            DatagramPacket udpPacket;

                            byte[] buffer = new byte[2048];
                            // Create a packet to receive data into the buffer
                            udpPacket = new DatagramPacket(buffer, buffer.length);


                            udpSocket = udpserver.socket();

                            receiveBuffer.clear();

                            InetSocketAddress clientAddress = (InetSocketAddress) udpserver.receive(receiveBuffer);

                            if (clientAddress != null)
                            {

                                String fromAddress = clientAddress.getAddress().getHostAddress();

                                packetCounter++;


                                int received = receiveBuffer.position();
                                byte[] bufferConvert = new byte[received];

                                System.arraycopy(receiveBuffer.array(), 0, bufferConvert, 0, bufferConvert.length);

                                Packet storepacket = new Packet();
                                storepacket.tcpOrUdp = "UDP";
                                storepacket.fromIP = clientAddress.getAddress().getHostAddress();

                                storepacket.toIP = "You";
                                storepacket.fromPort = clientAddress.getPort();
                                storepacket.port = udpSocket.getLocalPort();
                                storepacket.data = bufferConvert;

                                UpdateNotification("UDP:" + storepacket.toAscii(), "From " + storepacket.fromIP);

                                //dataStore.SavePacket(storepacket);
                               storepacket.nowMe();
                               dataStore.saveTrafficPacket(storepacket);
                                Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));


                                if(false)//mDbHelper.getSettings(PSDbAdapter.KEY_SETTINGS_SENDRESPONSE).trim().equalsIgnoreCase("Yes"))
                                {
                                    storepacket = new Packet();
                                    storepacket.name = dataStore.currentTimeStamp();;
                                    storepacket.tcpOrUdp = "UDP";
                                    storepacket.fromIP = "You";
                                    storepacket.toIP = clientAddress.getAddress().getHostAddress();
                                    storepacket.fromPort = udpSocket.getLocalPort();
                                    storepacket.port = clientAddress.getPort();
                                   // storepacket.data = Packet.toBytes(mDbHelper.getSettings(PSDbAdapter.KEY_SETTINGS_SENDRESPONSETEXT));


                                    //dataStore.SavePacket(storepacket);
                                    udpserver.send(response, clientAddress);
                                    storepacket.nowMe();
                                    dataStore.saveTrafficPacket(storepacket);
                                    Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));


                                }
                            }
                        }
                    }
                } catch (java.io.IOException e) {
                    Log.i("service",DataStorage.FILE_LINE("IOException "));
                } catch (Exception e) {
                    Log.w("service",DataStorage.FILE_LINE("Fatal Error: "  + Log.getStackTraceString(e)));
                }
            }
        }  catch (BindException e) {

            //mDbHelper.putServiceError("Error binding to port");
            dataStore.putToast("Port already in use.");
            Log.w("service",DataStorage.FILE_LINE("Bind Exception: " + Log.getStackTraceString(e)));


        } catch (Exception e) {
            //mDbHelper.putServiceError("Fatal Error starting service");
            Log.w("service",DataStorage.FILE_LINE("Startup error: "  + Log.getStackTraceString(e)));
        }


        stopNotification();











    }



    public void onDestroy ()
    {

        try {
            Log.w("service",DataStorage.FILE_LINE("Closing connections"));
            tcpserver.close();
            udpserver.close();
        } catch (IOException e) {
            Log.w("service",DataStorage.FILE_LINE("IOException error: "  + Log.getStackTraceString(e)));
        }  catch (NullPointerException e){
            Log.w("service",DataStorage.FILE_LINE("NullPointerException error: "  + Log.getStackTraceString(e)));
        }

        stopNotification();

    }
    public void startNotification () {

        UpdateNotification("TCP: " + listenportTCP + " / UDP: " + listenportUDP, "Packet Sender Active");

    }


    public void stopNotification () {

        mNM.cancelAll();

    }
    public void UpdateNotification(String title, String subject) {

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_status24)
                        .setLargeIcon(bm)
                        .setContentTitle(title)
                        .setTicker(title)
                        .setContentText(subject);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(NOTIFICATIONID, builder.build());


    }


    private class SendPacketsTask extends AsyncTask<Packet, Void, Void> {
        // Do the long-running work in here
        protected Void doInBackground(Packet... params) {

            Log.d("SendPacketsTask", DataStorage.FILE_LINE("length" + params.length));

            Packet fetchedPacket = params[0];
            Log.d("SendPacketsTask", DataStorage.FILE_LINE("send packet " + fetchedPacket.toString()));

            //if(1+1 == 2) return null;
            if(fetchedPacket.tcpOrUdp.equalsIgnoreCase("tcp")) {
                UpdateNotification("Send TCP: " + fetchedPacket.toIP, ":" + fetchedPacket.port);

            } else {
                UpdateNotification("Send UDP: " + fetchedPacket.toIP, ":" + fetchedPacket.port);


                //dataStore.SavePacket(storepacket);
                try {

                    ByteBuffer buf = ByteBuffer.allocate(fetchedPacket.data.length);
                    buf.clear();
                    buf.put(fetchedPacket.data);
                    buf.flip();

                    udpserver.send(buf, new InetSocketAddress(fetchedPacket.toIP, fetchedPacket.port));


                    fetchedPacket.fromIP = "You";
                    fetchedPacket.nowMe();
                    dataStore.saveTrafficPacket(fetchedPacket);
                    Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));



                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;


            }



            try {
                Socket clientSocket;
                InetSocketAddress translateSocket = new InetSocketAddress(fetchedPacket.toIP, fetchedPacket.port);
                clientSocket = new Socket(translateSocket.getHostName(), translateSocket.getPort());
                clientSocket.setSoTimeout(2000); // 2 second timeout

                DataOutputStream out = new DataOutputStream(
                        clientSocket.getOutputStream());

                DataInputStream in = new DataInputStream(
                        clientSocket.getInputStream());

                out.write(fetchedPacket.data);


                Packet savePacket = fetchedPacket.duplicate();
                savePacket.nowMe();
                savePacket.fromIP = "You";
                fetchedPacket.nowMe();
                dataStore.saveTrafficPacket(savePacket);
                Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));


                byte[] buffer = new byte[1024];
                int received = in.read(buffer);
                if(received > 0) {
                    byte[] bufferConvert = new byte[received];
                    System.arraycopy(buffer, 0, bufferConvert, 0, received);
                    Log.i("service",DataStorage.FILE_LINE("FROM SERVER: " + Packet.toHex(bufferConvert)));

                    savePacket = fetchedPacket.duplicate();
                    savePacket.nowMe();
                    savePacket.data = bufferConvert;
                    savePacket.fromIP = fetchedPacket.toIP;
                    savePacket.fromPort = fetchedPacket.port;
                    savePacket.toIP = "You";
                    savePacket.port = fetchedPacket.fromPort;

                    fetchedPacket.nowMe();
                    dataStore.saveTrafficPacket(savePacket);
                    Log.d("service", DataStorage.FILE_LINE("sendBroadcast"));


                }

                out.close();
                in.close();
                clientSocket.close();

            } catch (SocketTimeoutException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.i("service", DataStorage.FILE_LINE("SocketTimeoutException: " + sw.toString()));

            } catch (NetworkOnMainThreadException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service",DataStorage.FILE_LINE("NetworkOnMainThreadException: " + sw.toString()));

            } catch (UnknownHostException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.i("service", DataStorage.FILE_LINE("UnknownHostException: " + sw.toString()));
                Log.i("service", DataStorage.FILE_LINE("Saving the error to packet."));
                fetchedPacket.errorString = "Unknown host";
                fetchedPacket.nowMe();
                dataStore.saveTrafficPacket(fetchedPacket);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service", DataStorage.FILE_LINE("IOException: " + sw.toString())); //failed to connect error.
                Log.i("service", DataStorage.FILE_LINE("Saving the error to packet."));
                fetchedPacket.errorString = "Connection error";
                fetchedPacket.nowMe();
                dataStore.saveTrafficPacket(fetchedPacket);

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service",DataStorage.FILE_LINE("Exception: " + sw.toString()));


            }


            return null;
        }


        protected void onProgressUpdate()
        {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(boolean result)
        {
            //showDialog("Downloaded " + result + " bytes");
        }
    }


}
