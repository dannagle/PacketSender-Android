//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
public class MainActivity extends Activity
        implements ActionBar.TabListener, SearchView.OnQueryTextListener
{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    private static Intent serviceIntent;
    private Context mContext;
    private DataStorage dataStore;

    private int activeMenu = R.menu.packetlistmenu;

    private SearchView mSearchView;


    private List<Packet> trafficLogPackets;
    public View trafficFragmentView;
    public View packetsFragmentView;

    //Runnable to self update the saved lists...
    private Runnable updateSavedLists;


    private String ipAddress;
    private boolean wifiActive;
    private Runnable updateWifi;
    private Runnable trafficLogPolling;

    private final Handler mHandler = new Handler();

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    public void onResume() {
        super.onResume();
        if(packetsFragmentView != null) {
            packetsFragmentView.invalidate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    public void onDestroy() {

        stopListenerService();
        super.onDestroy();
    }

    @Override
    public boolean onQueryTextSubmit(String s) {


        Log.d("main", DataStorage.FILE_LINE(s));

        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        Log.d("main", DataStorage.FILE_LINE(s));

        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext =  getApplicationContext();
        serviceIntent = new Intent(mContext, PacketListenerService.class);
        activeMenu = R.menu.packetlistmenu;


        trafficLogPackets = new ArrayList<Packet>();

        dataStore = new DataStorage(
                getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
        );

        dataStore.clearServicePackets();

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }


        startListenerService();

        wifiActive = DataStorage.isWifiActive(mContext);
        ipAddress = "";

        if(wifiActive) {
            ipAddress = DataStorage.getIP(mContext);
            Toast.makeText(mContext, "Your IP is " + ipAddress, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Send only. Wifi is inactive.", Toast.LENGTH_LONG).show();
        }

        //periodically poll the traffic log

        trafficLogPolling = new Runnable() {
            public void run() {

                String msg = dataStore.getToast();
                if(!msg.isEmpty()) {
                    Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                }


                Packet[] trafficPackets = dataStore.fetchAllTrafficLogPackets();
                if(trafficPackets.length != trafficLogPackets.size()) {
                    trafficLogPackets.clear();
                    trafficLogPackets.addAll(Arrays.asList(trafficPackets));
                    updateTrafficPacketsList(trafficFragmentView);
                    //trafficFragmentView

                }

                //Log.d("main", DataStorage.FILE_LINE( "trafficLogPolling."));
                mHandler.postDelayed(trafficLogPolling,1100);
            }
        };


        //periodically monitor Wi-Fi
        updateWifi = new Runnable() {
            public void run() {

                boolean checkWifi = DataStorage.isWifiActive(mContext);
                if(checkWifi != wifiActive) {
                    if(checkWifi) {
                        ipAddress = DataStorage.getIP(mContext);
                        Toast.makeText(mContext, "Your IP is " + ipAddress, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(mContext, "Wifi is not active", Toast.LENGTH_LONG).show();
                    }

                    wifiActive = checkWifi;

                }
                mHandler.postDelayed(updateWifi,5000);
            }
        };

        mHandler.postDelayed(updateWifi,5000);
        mHandler.postDelayed(trafficLogPolling, 700);
        //setup saved List periodic check
        updateSavedLists = new Runnable() {
            public void run() {
                if(dataStore.isInvalidateLists()) {

                    Log.d("main", DataStorage.FILE_LINE( "Found invalid lists."));
                    updateSavedPacketsList(packetsFragmentView);
                    dataStore.clearInvalidateLists();
                }

                mHandler.postDelayed(updateSavedLists,2000);
            }
        };

        mHandler.postDelayed(updateSavedLists,7000);







    }

    public AdapterView.OnItemClickListener getTrafficOnClick() {

        final MainActivity Main = this;

        return new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final int finalPosition = position;
                Log.d("main", DataStorage.FILE_LINE("Clicked "  + position));
                final Packet updatePacket = trafficLogPackets.get(position).duplicate();

                updatePacket.name = "save";
                AlertDialog.Builder alert = new AlertDialog.Builder(Main);
                alert.setTitle("Packet Name?");


                // Set an EditText view to get user input
                final EditText input = new EditText(Main);

                input.setInputType(InputType.TYPE_CLASS_TEXT); //);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        // Do something with value!
                        updatePacket.name = value;

                        Packet tempPacket = updatePacket;
                        updatePacket.port = tempPacket.fromPort;
                        updatePacket.toIP = tempPacket.fromIP;

                        ////mDBHelper.updatePacket(packet);
                        //mDBHelper.storemessage(packet);
                        dataStore.savePacket(updatePacket);
                        updateSavedPacketsList(packetsFragmentView);

                        Toast.makeText(mContext, "Reversed addresses and saved.", Toast.LENGTH_SHORT).show();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                // see http://androidsnippets.com/prompt-user-input-with-an-alertdialog



            }

        };

    }

    public void updateTrafficPacketsList(View rootView) {

        ListView trafficListView = (ListView) rootView.findViewById(R.id.trafficList);
        List<Packet> packetList = new ArrayList<Packet>();
        packetList.addAll(Arrays.asList( trafficLogPackets.toArray(new Packet[trafficLogPackets.size()])));
        Collections.sort(packetList);
        Collections.reverse(packetList);

        PacketAdapter packetAdapter = new PacketAdapter(this, packetList.toArray(new Packet[packetList.size()]));


        if(trafficListView != null) {

            trafficListView.setAdapter(packetAdapter);
            packetAdapter.notifyDataSetChanged();

            trafficListView.setOnItemClickListener(getTrafficOnClick());

            TextView trafficLogEmptyText =
                    (TextView) rootView.findViewById(R.id.trafficLogEmptyText);

            Button clearLogButton = (Button) rootView.findViewById(R.id.clearTrafficLogButton);

            if(packetAdapter.isEmpty()) {
                trafficLogEmptyText.setVisibility(View.VISIBLE);
                clearLogButton.setVisibility(View.GONE);
            } else {
                clearLogButton.setVisibility(View.VISIBLE);
                trafficLogEmptyText.setVisibility(View.GONE);
            }


        }
    }


    public void updateSavedPacketsList(View rootView) {

        ListView packetListView = (ListView) rootView.findViewById(R.id.packetList);


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

                    Log.d("main", DataStorage.FILE_LINE("Clicked "  + position));



                    Packet sendPacket = packetArray[position].duplicate();
                    if(activeMenu == R.menu.acceptchangemenu)  {
                        Log.d("main", DataStorage.FILE_LINE("Need to delete "  + sendPacket.name ));
                        dataStore.DeleteSavedPacket(sendPacket);
                        updateSavedPacketsList(packetsFragmentView);
                    } else {
                        Log.d("main", DataStorage.FILE_LINE("Need to send "  + sendPacket.name + " data " + sendPacket.toAscii()));
                        dataStore.sendPacketToService(sendPacket);

                    }


                }

            });


            packetListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d("main", DataStorage.FILE_LINE("long click " + position));



                    Packet editPacket = packetArray[position].duplicate();
                    Intent newPacketActivity = DataStorage.getIntentFromPacket(editPacket);
                    newPacketActivity.setClass(getApplicationContext(), NewPacketActivity.class);
                    startActivity(newPacketActivity);

                    return true;
                }
            });


        }




    }


    public static boolean isMyServiceRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.packetsender.android.PacketListenerService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public RunningServiceInfo getServiceIntent() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.packetsender.android.PacketListenerService".equals(service.service.getClassName())) {
                return service;
            }
        }

        return null;
    }


    public void stopListenerService() {

        if(isMyServiceRunning(mContext))
        {
            Log.i("main",  DataStorage.FILE_LINE("Service is running. Stop it."));
            if(serviceIntent != null) {
                Log.d("debug",  DataStorage.FILE_LINE("serviceIntent is not null."));
            }

            mContext.stopService(serviceIntent);
        }

    }

    public void startListenerService() {

        if(isMyServiceRunning(mContext))
        {
            stopListenerService();
            // do nothing
        }   //this.needStop(); //clear out stop commands

        mContext.startService(serviceIntent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(activeMenu, menu);

        if(activeMenu == R.menu.acceptchangemenu) return  true;

        //MenuItem searchItem = menu.findItem(R.id.action_search);
       // mSearchView = (SearchView) searchItem.getActionView();
       // setupSearchView(searchItem);
        return true;
    }

    private void setupSearchView(MenuItem searchItem) {

        //searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            List<SearchableInfo> searchables = searchManager.getSearchablesInGlobalSearch();

            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            for (SearchableInfo inf : searchables) {
                if (inf.getSuggestAuthority() != null
                        && inf.getSuggestAuthority().startsWith("applications")) {
                    info = inf;
                }
            }
            mSearchView.setSearchableInfo(info);
        }

        mSearchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        activeMenu = R.menu.packetlistmenu;
        switch (id) {
            case R.id.action_new:

                Intent newPacketActivty = new Intent(this, NewPacketActivity.class);
                startActivity(newPacketActivty);

                break;
            case R.id.action_discard:
                Toast.makeText(this, "Tap to delete.", Toast.LENGTH_SHORT)
                        .show();
                activeMenu = R.menu.acceptchangemenu;
                break;
            case R.id.action_search:

                break;
            case R.id.action_about:

                Intent newAboutActivity = new Intent(this, AboutActivity.class);
                startActivity(newAboutActivity);

                break;

            default:
                break;
        }


        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_packets).toUpperCase(l);
                case 1:
                    return getString(R.string.title_trafficlog).toUpperCase(l);
                case 2:
                    return getString(R.string.title_settings).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */





        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {


            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

            MainActivity Main = (MainActivity)getActivity();

            switch((getArguments().getInt(ARG_SECTION_NUMBER)))
            {
                case 0:
                    break;
                case 1:
                    rootView = inflater.inflate(R.layout.packetlist, container, false);
                    Main.packetsFragmentView = rootView;

                    //load saved packets
                    Main.updateSavedPacketsList(rootView);

                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.trafficlist, container, false);
                    Main.trafficFragmentView = rootView;
                    //load traffic log packets
                    Main.updateTrafficPacketsList(rootView);


                    Button clearLogButton = (Button) rootView.findViewById(R.id.clearTrafficLogButton);
                    clearLogButton.setOnClickListener(new Button.OnClickListener() {
                        public void onClick(View v)
                        {
                            //perform action
                            MainActivity Main = (MainActivity)getActivity();
                            Main.dataStore.clearTrafficPackets();
                        }
                    });



                    break;
                case 3:
                    rootView = inflater.inflate(R.layout.settingsform, container, false);
                    //TODO actually implement the settings stuff

                    Button wifiButton = (Button) rootView.findViewById(R.id.setupWifiButton);
                    wifiButton.setOnClickListener(new Button.OnClickListener() {
                        public void onClick(View v)
                        {
                            //perform action
                            Log.d("main", DataStorage.FILE_LINE("Do wifiButton button"));
                            MainActivity Main = (MainActivity)getActivity();
                            Main.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                        }
                    });



                    Main.dataStore.prepSettings(rootView);

                    Button applyButton = (Button) rootView.findViewById(R.id.applyButton);
                    final View finalView = rootView;
                    applyButton.setOnClickListener(new Button.OnClickListener() {
                        public void onClick(View v)
                        {
                            //perform action
                            Log.d("main", DataStorage.FILE_LINE("Do apply button"));
                            MainActivity Main = (MainActivity)getActivity();
                            Main.dataStore.saveSettings(finalView);
                            Main.startListenerService();



                        }
                    });

                default :
                    break;





            }

            return rootView;
        }
    }

}
