//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class PSWidgetOnCreate extends Activity {


    Button configOkButton;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setResult(RESULT_CANCELED);


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        setContentView(R.layout.activity_pswidget_on_create);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }




    }

    private DataStorage dataStore;

    public void onCreatePacketsList(View rootView) {


        ListView packetListView = (ListView) rootView.findViewById(R.id.packetlist4widget);


        dataStore = new DataStorage(
                getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
        );


        final Packet[] packetArray =  dataStore.fetchAllSavedPackets();
        PacketAdapter packetAdapter = new PacketAdapter(this,packetArray);



        if(packetListView != null) {

            packetListView.setAdapter(packetAdapter);
            packetAdapter.notifyDataSetChanged();

            TextView noSavedPacketsText =
                    (TextView) rootView.findViewById(R.id.noSavedPacketsText);

            if(packetAdapter.isEmpty()) {
                noSavedPacketsText.setVisibility(View.VISIBLE);
            } else {
                noSavedPacketsText.setVisibility(View.GONE);
            }


            packetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {

                    Log.d("psoncreate", DataStorage.FILE_LINE("Clicked "  + position));
                    Packet sendPacket = packetArray[position].duplicate();
                    Log.d("psoncreate", DataStorage.FILE_LINE("Need to set widget to " + sendPacket.name));
                    dataStore.setWidgetPacket(mAppWidgetId, sendPacket.name);

                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    resultValue.setAction(PacketSenderWidget.WIDGET_CLICKED);
                    setResult(RESULT_OK, resultValue);

                    PacketSenderWidget.updateWidgets(getApplicationContext());
/*
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

                    Intent intent = new Intent(getApplicationContext(), PacketSenderWidget.class);
                    intent.setAction(PacketSenderWidget.WIDGET_CLICKED);
                    intent.putExtra(PacketSenderWidget.WIDGET_ID, mAppWidgetId);
                    PendingIntent selfIntent =
                            PendingIntent.getBroadcast(getApplicationContext(), mAppWidgetId, intent, PendingIntent.FLAG_ONE_SHOT);

                    RemoteViews remoteViews;
                    remoteViews = new RemoteViews(getPackageName(), R.layout.widget_layout);
                    remoteViews.setTextViewText(R.id.widget_textview, sendPacket.name);
                    remoteViews.setOnClickPendingIntent(R.id.widget_imagebutton,selfIntent );


                    if(sendPacket.tcpOrUdp.equalsIgnoreCase("tcp")) {
                        remoteViews.setImageViewResource(R.id.widget_imagebutton, R.drawable.tx_tcp);

                    } else {
                        remoteViews.setImageViewResource(R.id.widget_imagebutton, R.drawable.tx_udp);

                    }

                    appWidgetManager.updateAppWidget(mAppWidgetId, remoteViews);

*/

                    finish();

                }

            });




        }




    }



    private Button.OnClickListener configOkButtonOnClickListener
            = new Button.OnClickListener(){

        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub

            final Context context = PSWidgetOnCreate.this;

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.hellowidget_layout);
            //appWidgetManager.updateAppWidget(mAppWidgetId, views);
            //PacketSenderWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            Toast.makeText(context, "HelloWidgetConfig.onClick(): " + String.valueOf(mAppWidgetId) , Toast.LENGTH_LONG).show();

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            resultValue.setAction(PacketSenderWidget.WIDGET_CLICKED);

            setResult(RESULT_OK, resultValue);
            finish();
        }
    };


    /*

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pswidget_on_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pswidget_on_create, container, false);

            PSWidgetOnCreate PSOC = (PSWidgetOnCreate) getActivity();
            PSOC.configOkButton = (Button)rootView.findViewById(R.id.okconfig);
            PSOC.configOkButton.setOnClickListener(PSOC.configOkButtonOnClickListener);
            PSOC.configOkButton.setVisibility(View.GONE);
            PSOC.onCreatePacketsList(rootView);



            return rootView;
        }
    }

}
