//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class PacketAdapter extends ArrayAdapter<String> {

    private List<Packet> packets;
    private Context ctx;

    private static String[] getObjects(Packet[] thePackets) {

        String [] returnList = new String[thePackets.length];

        for(int i = 0 ; i<thePackets.length; i++)
        {
            returnList[i] = thePackets[i].name;
        }

        return returnList;
    }

    public PacketAdapter(Context context, Packet[] thePackets) {
        super(context, R.layout.packetrow, getObjects(thePackets));
        packets  = new ArrayList<Packet>();
        ctx = context;
        setList(thePackets);
    }

    public void clear() {
        packets.clear();
    }

    public void setList(Packet [] packetArray) {
        packets.clear();
        packets.addAll(Arrays.asList(packetArray));
        Log.d("adapter", DataStorage.FILE_LINE("List now has " + packets.size() + " packets"));
    }


    public void addPacket(Packet packet) {
        Log.d("adapter", DataStorage.FILE_LINE("Adding packet " + packet.name));
        packets.add(packet);
    }

    public boolean isEmpty() {

        return packets.isEmpty();

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.packetrow, parent, false);
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView errorLine = (TextView) rowView.findViewById(R.id.errorLine);
        TextView middleLine = (TextView) rowView.findViewById(R.id.middleLine);
        TextView secondline = (TextView) rowView.findViewById(R.id.secondLine);

        Packet packet = packets.get(position);

        long timestampTest = -1;
        try {
            timestampTest = Long.parseLong(packet.name);
        } catch (NumberFormatException e) {
            //Ignore.
            timestampTest = -1;
        }


        Log.d("adapter", DataStorage.FILE_LINE("Error string is " + packet.errorString));

        //Rather cheesy to detect traffic log: Setting the name as the timestamp
        //If name is a long between a reasonable millisecond epoch, then show a date.

        if(timestampTest > 1380000000000L && timestampTest < 9380000000000L) {
            firstLine.setText(packet.timestampString());
            if(packet.errorString.isEmpty()) {
                errorLine.setVisibility(View.GONE);
            } else {
                errorLine.setText(packet.errorString);
                errorLine.setVisibility(View.VISIBLE);
            }
        } else {
            firstLine.setText(packet.name);
            errorLine.setVisibility(View.GONE);
        }

        String ascii = packet.toAscii();
        if(ascii.length() > 75) {
            ascii = ascii.substring(0, Math.min(ascii.length(), 75)) + "...";
        }

        secondline.setText(ascii);
        // Change the icon for Windows and iPhone

        if(packet.toIP.equalsIgnoreCase("you")) {
            middleLine.setText(packet.fromIP + ":" + packet.fromPort + Html.fromHtml(" &#8594; ") + "You");
        }
        if(packet.fromIP.equalsIgnoreCase("you")) {
            middleLine.setText("You" + Html.fromHtml(" &#8594; ") + packet.toIP + ":" + packet.port);
        }

        ImageView icon = (ImageView) rowView.findViewById(R.id.icon);
        if(packet.tcpOrUdp.equalsIgnoreCase("udp")) {
            if(packet.toIP.equalsIgnoreCase("you")) {
                icon.setImageResource(R.drawable.rx_udp);

            } else {
                icon.setImageResource(R.drawable.tx_udp);
            }
        } else {
            if(packet.toIP.equalsIgnoreCase("you")) {
                icon.setImageResource(R.drawable.rx_tcp);
            } else {
                icon.setImageResource(R.drawable.tx_tcp);
            }
        }

        return rowView;
    }
}
