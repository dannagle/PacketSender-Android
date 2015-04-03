//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import java.net.Inet4Address;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class DataStorage {

    public SharedPreferences settings;
    public SharedPreferences servicelog;
    public SharedPreferences savedpackets;
    public SharedPreferences maintrafficlog;

    public static final String PREFS_SETTINGS_NAME = "PS_Settings";
    public static final String PREFS_SERVICELOG_NAME = "PS_Service";
    public static final String PREFS_MAINTRAFFICLOG_NAME = "PS_MainTraffic";
    public static final String PREFS_SAVEDPACKETS_NAME = "PS_Packets";

    public static final String INTENT_OUT = "packet_out";

    public static final String DATE_FORMAT = "hh:mm:ss.S a";

    DataStorage(SharedPreferences settings,
                SharedPreferences savedpackets,
                SharedPreferences servicelog,
                SharedPreferences maintrafficlog) {

        this.settings = settings;
        this.savedpackets = savedpackets;
        this.servicelog = servicelog;
        this.maintrafficlog = maintrafficlog;

    }

    public int getUDPPort() {

        int udp = settings.getInt("udpPort", 55057);
        return udp;
    }

    public int getTCPPort() {
        int tcp = settings.getInt("tcpPort", 55056);
        return tcp;
    }

    public boolean udpServerEnable() {
     return settings.getBoolean("enableUDPServer", true);
    }

    public boolean tcpServerEnable() {
        return settings.getBoolean("enableTCPServer", true);
    }

    public void putToast(String msg) {
        SharedPreferences.Editor editor = servicelog.edit();
        editor.putString("toToast", msg);
        editor.commit();
    }

    public String getToast() {
        String msg = servicelog.getString("toToast", "");
        putToast("");
        return msg;
    }

    public void sendPacketToService (Packet packet) {
        packet.timestamp = 0;
        packet.name = packet.now() + "";
        Log.d("store", "send packet " + packet.toString());
        depositPacket(packet, servicelog);
    }


    public void clearServicePackets() {
        SharedPreferences.Editor editor = servicelog.edit();
        editor.clear();
        editor.commit();
    }

    public void clearTrafficPackets() {
        SharedPreferences.Editor editor = maintrafficlog.edit();
        editor.clear();
        editor.commit();
    }

    public Packet getWidgetPacket(int ID) {

        String name = settings.getString("widget/ID/" + ID, "");
        if(name.isEmpty()) {
            return new Packet();
        }

        return fetchStoredPacketByName(name, savedpackets);

    }

    public void setWidgetPacket(int ID, String name) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("widget/ID/" + ID, name);
        editor.commit();

    }

    public Packet[] fetchAllTrafficLogPackets() {

        List<Packet> packetListL = new ArrayList<Packet>();
        packetListL.addAll(Arrays.asList(fetchAllPackets(maintrafficlog)));

        Collections.sort(packetListL);
        Collections.reverse(packetListL);
        return packetListL.toArray(new Packet[packetListL.size()]);

    }

    public Packet[] fetchAllServicePackets() {

        List<Packet> packetListL = new ArrayList<Packet>();
        packetListL.addAll(Arrays.asList(fetchAllPackets(servicelog)));

        Collections.sort(packetListL);

        for (Packet p : packetListL) {
         //   Log.d("store before", DataStorage.FILE_LINE( p.name));
        }

        Collections.reverse(packetListL);

        for (Packet p : packetListL) {
          //  Log.d("store before", DataStorage.FILE_LINE( p.name));
        }


        return packetListL.toArray(new Packet[packetListL.size()]);

        //TODO sort by timestamp
    }


    public void invalidateLists() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("invalidateLists", true);
        editor.commit();

        Log.d("store", DataStorage.FILE_LINE("settings lists invalid"));
    }
    public boolean isInvalidateLists() {
        return settings.getBoolean("invalidateLists", false);
    }
    public void clearInvalidateLists() {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("invalidateLists");
        editor.commit();
        Log.d("store", DataStorage.FILE_LINE("clear invalid lists."));
    }


    public void prepSettings(View rootView) {

        EditText tcpServerEditText = (EditText) rootView.findViewById(R.id.tcpServerEditText);
        EditText udpServerEditText =  (EditText) rootView.findViewById(R.id.udpServerEditText);
        CheckBox enableUDPServerCheck =  (CheckBox) rootView.findViewById(R.id.enableUDPServerCheck);
        CheckBox enableTCPServerCheck =  (CheckBox) rootView.findViewById(R.id.enableTCPServerCheck);

        tcpServerEditText.setText(getTCPPort() + "");
        udpServerEditText.setText(getUDPPort() + "");
        enableUDPServerCheck.setChecked(udpServerEnable());
        enableTCPServerCheck.setChecked(tcpServerEnable());
    }

    public static boolean isWifiActive(Context ctx)
    {
        WifiManager wifi = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);

        if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED )
        {

            return false;
        } else {

            if(getIP(ctx).equalsIgnoreCase("0.0.0.0"))
            {
                return false;

            } else {
                return true;
            }


        }
    }


    public static String getIP(Context ctx)
    {

        WifiManager wifi = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);

        if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED )
        {
            return "Wifi Disabled";

        } else {

            WifiInfo info = wifi.getConnectionInfo();
            int ipAddressInt = info.getIpAddress();

            //convert it to normal dot notation
            String ipBulder = ""+ (((ipAddressInt >>> 24) & 0xFF));
            ipBulder = (((ipAddressInt >>> 16) & 0xFF)) + "." + ipBulder;
            ipBulder = (((ipAddressInt >>> 8) & 0xFF)) + "." + ipBulder;
            ipBulder =  ((ipAddressInt & 0xFF)) + "." + ipBulder;

            return ipBulder.trim();


        }

    }

    public void saveSettings(View rootView) {

        EditText tcpServerEditText = (EditText) rootView.findViewById(R.id.tcpServerEditText);
        EditText udpServerEditText =  (EditText) rootView.findViewById(R.id.udpServerEditText);
        CheckBox enableUDPServerCheck =  (CheckBox) rootView.findViewById(R.id.enableUDPServerCheck);
        CheckBox enableTCPServerCheck =  (CheckBox) rootView.findViewById(R.id.enableTCPServerCheck);


        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("tcpPort", Integer.parseInt(tcpServerEditText.getText().toString()));
        editor.putInt("udpPort", Integer.parseInt(udpServerEditText.getText().toString()));
        editor.putBoolean("enableUDPServer", enableUDPServerCheck.isChecked());
        editor.putBoolean("enableTCPServer", enableTCPServerCheck.isChecked());
        editor.commit();
    }

    public void DeleteSavedPacket(Packet packet) {
        DeletePacket(packet, savedpackets);
    }

    public void DeletePacket(Packet packet, SharedPreferences prefs) {

        //find packet first...
        Packet findPacket = fetchStoredPacketByName(packet.name, prefs);
        if(findPacket.name.isEmpty()) {
            return; // packet does not exit
        }


        Packet [] packetListMain = fetchAllPackets(prefs);
        List<Packet> packetListNew = new ArrayList<Packet>();
        for(int i = 0; i < packetListMain.length; i++) {
            findPacket =  packetListMain[i];
            if(findPacket.name.equalsIgnoreCase(packet.name)) {
                continue;
            }
            packetListNew.add(findPacket);
        }
        SavePacketList(packetListNew.toArray(new Packet[packetListNew.size()]), prefs);



    }

    private void SavePacketList(Packet[] packetListNew, SharedPreferences prefs) {

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putInt("packetsSize", packetListNew.length);
        Log.d("store", "packetsSize is " + packetListNew.length);

        for(int i = 0; i < packetListNew.length; i++) {
            Packet packet =  packetListNew[i];
            storePacket(packet,i, editor);
        }
        editor.commit();



    }

    private Packet fetchStoredPacketByName(String packetName, SharedPreferences prefs) {

        Packet returnPacket = new Packet();

        returnPacket.name = prefs.getString(packetName + "/name", "");;
        returnPacket.data = Packet.toBytes(prefs.getString(packetName + "/data", ""));
        returnPacket.fromIP = prefs.getString(packetName + "/fromIP", "");
        returnPacket.toIP = prefs.getString(packetName + "/toIP", "");
        returnPacket.repeat = prefs.getInt(packetName + "/repeat", 0);
        returnPacket.port = prefs.getInt(packetName + "/port", 0);
        returnPacket.fromPort = prefs.getInt(packetName + "/fromPort", 0);
        returnPacket.tcpOrUdp = prefs.getString(packetName + "/tcpOrUdp", "UDP");
        returnPacket.timestamp = prefs.getLong(packetName + "/timestamp", 0);
        returnPacket.errorString = prefs.getString(packetName + "/error", "");

        return  returnPacket;
    }

    private void storePacket(Packet packet, int id, SharedPreferences.Editor editor) {
        String packetName = packet.name;
        Log.d("store", "saving packet " + packetName);
        if(id > -1) {
            editor.putString(id + "/packetid", packet.name);
        }
        editor.putString(packetName + "/name", packet.name);
        editor.putString(packetName + "/data", Packet.toHex(packet.data));
        editor.putString(packetName + "/fromIP", packet.fromIP);
        editor.putString(packetName + "/toIP", packet.toIP);
        editor.putInt(packetName + "/repeat", packet.repeat);
        editor.putInt(packetName + "/port", packet.port);
        editor.putInt(packetName + "/fromPort", packet.fromPort);
        editor.putString(packetName + "/tcpOrUdp", packet.tcpOrUdp);
        editor.putLong(packetName + "/timestamp", packet.timestamp);
        editor.putString(packetName + "/error", packet.errorString);
    }


    public static String currentTimeStamp() {
        Calendar cal = Calendar.getInstance();
        cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        return sdf.format(cal.getTime()).toLowerCase(Locale.US);
    }

    public void depositPacket(Packet packet, SharedPreferences prefs) {

        if(packet.name.isEmpty()) return;

        //find packet first...
        String packetName = prefs.getString(packet.name + "/name", "");
        if(packetName.isEmpty()) {
            //packet does not exit;
            Log.d("store", "Packet name " + packet.name + " does not exist");
            Packet [] packetListMain = fetchAllPackets(prefs);
            Packet [] packetListNew = new Packet[packetListMain.length + 1];
            for(int i = 0; i < packetListNew.length; i++) {

                if(i < packetListMain.length ) {
                    packetListNew[i] = packetListMain[i];
                } else {
                    packetListNew[i] = packet;
                }
            }

            Log.d("store", "Saving new packet list of size " + packetListNew.length);
            SavePacketList(packetListNew, prefs);



        } else {
            Log.d("store", "Packet name " + packet.name + " exist. Overwrite");

            SharedPreferences.Editor editor = prefs.edit();
            storePacket(packet,-1, editor);
            editor.commit();
        }


    }

    public static String FILE_LINE(String msg)
    {
        return new Throwable().getStackTrace()[1].getFileName() +
         //       "/" + new Throwable().getStackTrace()[1].getClassName() +
         //       "/" + new Throwable().getStackTrace()[1].getMethodName() +
                "(" + new Throwable().getStackTrace()[1].getLineNumber() + "):" + msg;
    }

    public void savePacket(Packet packet) {

        depositPacket(packet, savedpackets);
    }
/*

    public String name;
    public String fromIP;
    public String toIP;
    public String errorString;
    public byte[] response;
    public int repeat;
    public int port;
    public int fromPort;
    public String tcpOrUdp;
    public byte[] data;
    public long timestamp;


*/


    public Packet[] fetchAllPackets(SharedPreferences prefs) {

        int packetsSize = prefs.getInt("packetsSize", 0);
        Packet[] packetList = new Packet[packetsSize];


        for(int i= 0; i < packetList.length; i++) {

            Packet returnPacket = new Packet();
            String packetName = prefs.getString(i + "/packetid", "");
            if(!packetName.isEmpty()) {

                returnPacket = fetchStoredPacketByName(packetName, prefs);
            }
            packetList[i] = returnPacket;

        }


        //TODO sort by name




        return packetList;

    }

    void saveTrafficPacket(Packet packet) {
        depositPacket(packet, maintrafficlog);
    }

    static public Packet getPacketFromIntent ( Intent intent) {
        Packet packet = new Packet();

        packet.name = intent.getStringExtra(INTENT_OUT + "/name");
        packet.toIP = intent.getStringExtra(INTENT_OUT + "/toIP");
        packet.fromIP = intent.getStringExtra(INTENT_OUT + "/fromIP");
        packet.port = intent.getIntExtra(INTENT_OUT + "/port", 0);
        packet.tcpOrUdp = intent.getStringExtra(INTENT_OUT + "/tcpOrUdp");
        packet.fromPort = intent.getIntExtra(INTENT_OUT + "/fromPort", 0);
        packet.data = Packet.toBytes(intent.getStringExtra(INTENT_OUT + "/data"));


        return packet;
    }


    static public Intent populateIntentFromPacket (Intent intent, Packet packet) {

        intent.putExtra(INTENT_OUT + "/name", packet.name);
        intent.putExtra(INTENT_OUT + "/toIP", packet.toIP);
        intent.putExtra(INTENT_OUT + "/fromIP", packet.fromIP);
        intent.putExtra(INTENT_OUT + "/port", packet.port);
        intent.putExtra(INTENT_OUT + "/tcpOrUdp", packet.tcpOrUdp);
        intent.putExtra(INTENT_OUT + "/fromPort", packet.fromPort);
        intent.putExtra(INTENT_OUT + "/data", Packet.toHex(packet.data));

        return intent;
    }


    static public Intent getIntentFromPacket (Packet packet) {

        Intent intent = new Intent();
        return populateIntentFromPacket (intent, packet);
    }

    public Packet[] fetchAllSavedPackets() {
        Packet[] packetList = fetchAllPackets(savedpackets);


        List<Packet> packetListL = new ArrayList<Packet>();


        for(int i= 0; i < packetList.length; i++) {
            packetList[i].fromIP = "You";
            packetListL.add(packetList[i]);
        }


        Collections.sort(packetListL);

        return packetListL.toArray(new Packet[packetListL.size()]);
    }
}
