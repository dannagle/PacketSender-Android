//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Packet implements Comparable<Packet> {


    /*

    QString name;
    QString hexString;
    QString fromIP;
    QString toIP;
    QString errorString;
    QByteArray response;
    unsigned int repeat;
    unsigned int port;
    unsigned int fromPort;
    QString tcpOrUdp;
    unsigned int sendResponse;
    void init();
    void clear();
    QDateTime timestamp;
     */

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


    public Packet(String name) {
        this.name = name;
    }

    public static long now()
    {
        return new Date().getTime();

    }

    public void nowMe() {
        this.timestamp = now();
        this.name = Long.toString(this.timestamp);
    }

    public String timestampString() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(DataStorage.DATE_FORMAT, Locale.US);
        return sdf.format(cal.getTime()).toLowerCase(Locale.US);
    }


    public Packet() {
        this.name = "";
        this.tcpOrUdp = "TCP";
        this.timestamp = now();
        this.fromIP = "192.168.1.23";
        this.toIP = "192.168.1.23";
        this.fromPort = 50055;
        this.port = 50055;
        this.data = new byte[10];
        this.errorString = "";
    }

    public Packet duplicate() {

        Packet newPacket = new Packet();

        newPacket.name = name;
        newPacket.fromIP = fromIP;
        newPacket.toIP = toIP;
        newPacket.errorString = errorString;
        newPacket.response = response;
        newPacket.repeat = repeat;
        newPacket.port = port;
        newPacket.fromPort = fromPort;
        newPacket.tcpOrUdp = tcpOrUdp;
        newPacket.data = new byte[data.length];
        System.arraycopy( data, 0, newPacket.data, 0, data.length );
        newPacket.timestamp = timestamp;

        return newPacket;

    }

    public String toString()
    {
        return "name:" + this.name +"\n" +
                "tcpudp:" + this.tcpOrUdp + "\n" +
                "timestamp:" + this.timestamp+  "\n" +
                "fromip:" + this.fromIP + "\n" +
                "toip:" + this.toIP + "\n" +
                "fromport:" + this.fromPort +  "\n" +
                "port:" + this.port + "\n" +
                "errorString:" + this.errorString + "\n" +
                "ascii:" + toAscii() + "\n" +
                "hex:" + toHex();

    }

    public static String toAscii(byte[] data)
    {
        Packet pkt = new Packet();
        pkt.data = data;

        return pkt.toAscii();

    }

    public String toAscii()
    {
        String returnString = "";
        for (int item : this.data)
        {
            if(item == 0x0A)
            {
                returnString = returnString + "\\n";

            } else if (item == 0x0D) {
                returnString = returnString + "\\r";

            } else if (item >= 0x20 && item <=  0x7E) {
                returnString = returnString +  (char) item;
            } else {
                String hex = Integer.toHexString(item & 0xff);
                if(hex.length() == 1) {
                    hex = "0" + hex;
                }
                returnString = returnString + "\\" + hex;
            }
        }

        return returnString;
    }

    private static int iAt(String s, int index) {
        if(index < s.length()) {
            return (((int) s.charAt(index)) & 0xff);
        } else {
            return -1;
        }

    }

    private static byte[] toByteArray(List<Byte> list){
        byte[] ret = new byte[list.size()];
        for(int i = 0;i < ret.length;i++)
            ret[i] = list.get(i);
        return ret;
    }

    public static byte[] asciiToBytes(String ascii) {

        int val1, val2, val3;
        char c1, c2;

        List<Byte> bytes = new ArrayList<Byte>();

        ascii = ascii.replace("\\r", "\\0d");
        ascii = ascii.replace("\\n", "\\0a");

        for(int i=0; i < ascii.length(); i++) {
            val1 = iAt(ascii, i);
            if (val1 >= 0x20 && val1 <=  0x7E) {

                if(val1 == (((int) '\\') & 0xff)) {
                    val2 = iAt(ascii, i+1);
                    val3 = iAt(ascii, i+2);
                    if(val2 > -1 && val3 > -1) {
                        c1 = ascii.charAt(i+1);
                        c2 = ascii.charAt(i+2);


                        try {
                            Log.d("packet", DataStorage.FILE_LINE("c1,c2 " + c1 + "," + c2));
                            Log.d("packet", DataStorage.FILE_LINE("val2,val3 " + val2 + "," + val3));
                            val2 = Integer.parseInt((c1 + "") + (c2 + ""), 16) & 0xff;
                            val3 = 0;
                            Log.d("packet", DataStorage.FILE_LINE("val2,val3 " + val2 + "," + val3));

                            bytes.add((byte)(val2 + val3));

                        } catch (NumberFormatException e) {
                            Log.d("packet", DataStorage.FILE_LINE("bad conversion"));

                        }

                        i += 2;
                        continue;
                    }

                } else {
                    val1 = iAt(ascii, i);
                    bytes.add((byte)(val1));

                }



            }

        }

        return toByteArray(bytes);
    }

    public static void UnitTest_conversions() {

        String testS[] = new String[2];
        String resultS[] = new String[testS.length];
        boolean tohex[] = new boolean[testS.length];

        testS[0] = "help\\25";
        resultS[0] = "68 65 6c 70 25".toUpperCase(Locale.US);
        tohex[0] = true;

        testS[1] = "68 65 6c 70 25";
        resultS[1] = "help%";
        tohex[1] = false;


        String test;
        for(int i=0; i < testS.length; i++) {
            if(tohex[i]) {
                test = toHex(asciiToBytes(testS[i]));
            } else {
                test = toAscii(toBytes(testS[i]));
            }

            Log.d("packet", DataStorage.FILE_LINE(test + " == " + resultS[i] + " ?"));

            if(test.equalsIgnoreCase(resultS[i])) {
                //passed
                Log.d("packet", DataStorage.FILE_LINE("pass, test " + i));

            } else {
                Log.d("packet", DataStorage.FILE_LINE("error, " + test.length() + "/" + resultS[i].length()
                        + " test " + i + ", got\n"
                        + test + "\nexpected:\n" + resultS[i] + "\n"));

            }
        }


    }



    public String toHex()
    {
        return Packet.toHex(this.data);
    }

    public static String toHex(byte[] bytes) {
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<bytes.length;i++)
        {
            String hex = Integer.toHexString(0xFF & bytes[i]).toUpperCase(Locale.US);
            if (hex.length() == 1)
            {
                // could use a for loop, but we're only dealing with a single byte
                hexString.append('0');
            }
            hexString.append(hex);
            hexString.append(' ');

        }
        return hexString.toString().trim();
    }

    public int compareTo(Packet that)
    {

        return this.name.compareTo(that.name);


    }

    public static byte[] toBytes(String param) {

        String delims = "[ ]+";
        String[] tokens = param.split(delims);

        byte[] returnBytes = new byte[tokens.length];
        for (int i=0;i<tokens.length;i++)
        {
            byte hex = 0;
            try {
                hex = (byte) (Integer.parseInt(tokens[i], 16) & 0xff );
            } catch (NumberFormatException nfe) {

                returnBytes[i] = 0;

                continue;
            }

            returnBytes[i] = hex;

        }
        return returnBytes;
    }
}
