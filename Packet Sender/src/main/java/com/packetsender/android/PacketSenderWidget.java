//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.app.Service;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.packetsender.android.MainActivity.isMyServiceRunning;

public class PacketSenderWidget extends AppWidgetProvider {


    public static final String WIDGET_CLICKED    = "com.packetsender.android.widgetclicked";
    public static final String WIDGET_ID    = "com.packetsender.android.widgetid";


    public DataStorage dataStore;



    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // TODO Auto-generated method stub
        super.onDeleted(context, appWidgetIds);
        //Toast.makeText(context, "onDeleted()", Toast.LENGTH_LONG).show();
        updateWidgets(context);
    }

    @Override
    public void onDisabled(Context context) {
        // TODO Auto-generated method stub
        super.onDisabled(context);
        //Toast.makeText(context, "onDisabled()", Toast.LENGTH_LONG).show();
        updateWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        // TODO Auto-generated method stub
        super.onEnabled(context);
        //Toast.makeText(context, "onEnabled()", Toast.LENGTH_LONG).show();
        updateWidgets(context);


    }

    private static AppWidgetManager getAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    private static int[] getAppWidgetIds(Context context) {
        int[] appWidgetIds = getAppWidgetManager(context).getAppWidgetIds(new ComponentName(context, PacketSenderWidget.class));
        return appWidgetIds;
    }

    public static void updateWidgets(Context context) {

        RemoteViews remoteViews;
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        AppWidgetManager appWidgetManager = getAppWidgetManager(context);
        int[] appWidgetIds = getAppWidgetIds(context);


        for(int ID : appWidgetIds ){
            Log.d("widget", DataStorage.FILE_LINE("Do the on update for " + ID));

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);


            DataStorage dataStore = new DataStorage(
                    context.getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
            );

            Packet widgetPacket = dataStore.getWidgetPacket(ID);



            //PendingIntent selfIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setTextViewText(R.id.widget_textview, widgetPacket.name);
            if(widgetPacket.tcpOrUdp.equalsIgnoreCase("tcp")) {
                remoteViews.setImageViewResource(R.id.widget_imagebutton, R.drawable.tx_tcp);

            } else {
                remoteViews.setImageViewResource(R.id.widget_imagebutton, R.drawable.tx_udp);

            }


            Intent intent = new Intent(context, PacketSenderWidget.class);
            intent.setAction(PacketSenderWidget.WIDGET_CLICKED);
            intent.putExtra(PacketSenderWidget.WIDGET_ID, ID);
            PendingIntent selfIntent = PendingIntent.getBroadcast(context, ID, intent, PendingIntent.FLAG_ONE_SHOT);

            remoteViews.setTextViewText(R.id.widget_textview, widgetPacket.name);
            remoteViews.setOnClickPendingIntent(R.id.widget_imagebutton, selfIntent);

            appWidgetManager.updateAppWidget(ID, remoteViews);


        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        updateWidgets(context);

    }


    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        Log.d("widget", DataStorage.FILE_LINE("Do the onreceive"));

        if (WIDGET_CLICKED.equals(intent.getAction())) {

            int extra = intent.getIntExtra(WIDGET_ID, 0);
            Log.d("widget", DataStorage.FILE_LINE("extra is " + extra));


            dataStore = new DataStorage(
                    context.getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                    context.getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
            );



            Packet sendPacket = dataStore.getWidgetPacket(extra);
            sendPacket.nowMe();

            Log.i("widget", DataStorage.FILE_LINE("widget says to send " + sendPacket.name));

            //launching service is apparently very unreliable. Do the update here.

            new SendPacketsTask().execute(sendPacket);

            updateWidgets(context);


            /*
            dataStore.sendPacketToService(sendPacket);

            if(!isMyServiceRunning(context))
            {
                Intent serviceIntent = new Intent(context, PacketListenerService.class);
                context.startService(serviceIntent);
            }

            */






        }
    }


    private class SendPacketsTask extends AsyncTask<Packet, Void, Void> {
        // Do the long-running work in here
        protected Void doInBackground(Packet... params) {

            Log.d("SendPacketsTask", DataStorage.FILE_LINE("length" + params.length));

            Packet fetchedPacket = params[0];
            Log.d("SendPacketsTask", DataStorage.FILE_LINE("send packet " + fetchedPacket.toString()));

            //if(1+1 == 2) return null;
            if(fetchedPacket.tcpOrUdp.equalsIgnoreCase("tcp")) {


            } else {


                //dataStore.SavePacket(storepacket);
                try {

                    ByteBuffer buf = ByteBuffer.allocate(fetchedPacket.data.length);
                    buf.clear();
                    buf.put(fetchedPacket.data);
                    buf.flip();


                    DatagramSocket udpSocket;
                    DatagramPacket udpPacket;

                    byte[] buffer = new byte[2048];
                    // Create a packet to receive data into the buffer
                    udpPacket = new DatagramPacket(buf.array(), buf.array().length);
                    udpSocket = new DatagramSocket(null);
                    udpSocket.setReuseAddress(true);

                    InetSocketAddress clientAddress =  new InetSocketAddress(fetchedPacket.toIP, fetchedPacket.port);
                    udpSocket.connect(clientAddress);
                    udpSocket.send(udpPacket);
                    udpSocket.close();

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
                clientSocket.setReuseAddress(true);
                clientSocket.setSoTimeout(2000); // 2 second timeout

                DataOutputStream out = new DataOutputStream(
                        clientSocket.getOutputStream());

                DataInputStream in = new DataInputStream(
                        clientSocket.getInputStream());

                out.write(fetchedPacket.data);


                Packet savePacket = fetchedPacket.duplicate();
                savePacket.fromIP = "You";
                savePacket.nowMe();
                dataStore.saveTrafficPacket(savePacket);
                Log.d("widget", DataStorage.FILE_LINE("sendBroadcast"));


                byte[] buffer = new byte[1024];
                int received = in.read(buffer);
                if(received > 0) {
                    byte[] bufferConvert = new byte[received];
                    System.arraycopy(buffer, 0, bufferConvert, 0, received);
                    Log.i("widget",DataStorage.FILE_LINE("FROM SERVER: " + Packet.toHex(bufferConvert)));

                    savePacket = fetchedPacket.duplicate();
                    savePacket.nowMe();
                    savePacket.data = bufferConvert;
                    savePacket.fromIP = fetchedPacket.toIP;
                    savePacket.fromPort = fetchedPacket.port;
                    savePacket.toIP = "You";
                    savePacket.port = fetchedPacket.fromPort;

                    fetchedPacket.nowMe();
                    dataStore.saveTrafficPacket(savePacket);
                    Log.d("widget", DataStorage.FILE_LINE("sendBroadcast"));


                }

                out.close();
                in.close();
                clientSocket.close();

            } catch (SocketTimeoutException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service",DataStorage.FILE_LINE("SocketTimeoutException: " + sw.toString()));

            } catch (NetworkOnMainThreadException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service",DataStorage.FILE_LINE("NetworkOnMainThreadException: " + sw.toString()));

            } catch (UnknownHostException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service", DataStorage.FILE_LINE("UnknownHostException: " + sw.toString()));
                Log.d("service",DataStorage.FILE_LINE("Saving the error to packet."));
                fetchedPacket.errorString = "Unknown host";
                fetchedPacket.nowMe();
                dataStore.saveTrafficPacket(fetchedPacket);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.w("service", DataStorage.FILE_LINE("IOException: " + sw.toString())); //failed to connect error.
                Log.d("service", DataStorage.FILE_LINE("Saving the error to packet."));
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
