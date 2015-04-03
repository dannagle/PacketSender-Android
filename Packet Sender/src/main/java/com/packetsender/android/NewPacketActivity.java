//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android
package com.packetsender.android;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class NewPacketActivity extends Activity {


    public DataStorage dataStore;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_packet);

        setTitle("Setup Packet");


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        dataStore = new DataStorage(
                getSharedPreferences(DataStorage.PREFS_SETTINGS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SAVEDPACKETS_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_SERVICELOG_NAME, 0),
                getSharedPreferences(DataStorage.PREFS_MAINTRAFFICLOG_NAME, 0)
        );

        mContext = getApplicationContext();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.new_packet, menu);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private EditText nameEditText;
        private EditText asciiEditText;
        private EditText hexEditText;
        private EditText ipEditText;
        private EditText portEditText;
        private Spinner methodSpinner;

        private Button testButton;
        private Button saveButton;


        private boolean suppressListener;

        public Packet getPacket() {

            Packet returnPacket = new Packet();

            returnPacket.name = nameEditText.getText().toString();
            returnPacket.data = returnPacket.toBytes(hexEditText.getText().toString());
            returnPacket.toIP = ipEditText.getText().toString();
            returnPacket.port = Integer.parseInt(portEditText.getText().toString());
            returnPacket.tcpOrUdp = methodSpinner.getSelectedItem().toString();



            return returnPacket;
        }


        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_packet, container, false);

            suppressListener = false;

            nameEditText = (EditText) rootView.findViewById(R.id.nameEditText);
            asciiEditText = (EditText) rootView.findViewById(R.id.asciiEditText);
            hexEditText = (EditText) rootView.findViewById(R.id.hexEditText);
            ipEditText = (EditText) rootView.findViewById(R.id.ipEditText);

            portEditText = (EditText) rootView.findViewById(R.id.portEditText);
            methodSpinner = (Spinner) rootView.findViewById(R.id.methodSpinner);

            testButton = (Button) rootView.findViewById(R.id.testButton);
            saveButton = (Button) rootView.findViewById(R.id.saveButton);


            NewPacketActivity NPA = (NewPacketActivity)getActivity();
            Intent passedIntent = NPA.getIntent();
            Packet receivedPacket = new Packet();
            receivedPacket.UnitTest_conversions();

            if(passedIntent.hasExtra(DataStorage.INTENT_OUT + "/name")) {
                receivedPacket = DataStorage.getPacketFromIntent(NPA.getIntent());
            }

            if(!receivedPacket.name.isEmpty()) {
                nameEditText.setText(receivedPacket.name);
                asciiEditText.setText(receivedPacket.toAscii());
                hexEditText.setText(receivedPacket.toHex());
                ipEditText.setText(receivedPacket.toIP);
                portEditText.setText(receivedPacket.port + "");
                if(receivedPacket.tcpOrUdp.equalsIgnoreCase("udp")) {
                    methodSpinner.setSelection(1);
                }

            }



            asciiEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {


                    if(suppressListener) return;

                    // TODO Auto-generated method stub
                    suppressListener = true;

                    String tohex = asciiEditText.getText().toString();
                    Log.d("newpacket", DataStorage.FILE_LINE(tohex));
                    byte [] bytes = Packet.asciiToBytes(tohex);
                    hexEditText.setText(Packet.toHex(bytes));

                    suppressListener = false;

                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    // TODO Auto-generated method stub
                }

                @Override
                public void afterTextChanged(Editable s) {

                    // TODO Auto-generated method stub
                }
            });



            hexEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    if(suppressListener) return;


                    // TODO Auto-generated method stub
                    suppressListener = true;

                    String toascii = hexEditText.getText().toString();
                    Log.d("newpacket", DataStorage.FILE_LINE(toascii));
                    byte [] bytes = Packet.toBytes(toascii);
                    asciiEditText.setText(Packet.toAscii(bytes));

                    suppressListener = false;
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    // TODO Auto-generated method stub
                }

                @Override
                public void afterTextChanged(Editable s) {

                    // TODO Auto-generated method stub
                }
            });










            testButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v)
                {
                    //perform action
                    Packet testPacket = getPacket();



                    Log.d("newpacket", DataStorage.FILE_LINE(testPacket + ""));


                    NewPacketActivity NPA = (NewPacketActivity)getActivity();
                    NPA.dataStore.sendPacketToService(testPacket);

                }
            });



            saveButton.setOnClickListener(new Button.OnClickListener() {

                public void onClick(View v)
                {
                    //perform action
                    Packet savePacket = getPacket();
                    if(savePacket.name.trim().isEmpty()) {

                        Toast.makeText((NewPacketActivity)getActivity(), "Name cannot be blank.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(savePacket.toIP.trim().isEmpty()) {

                        Toast.makeText((NewPacketActivity)getActivity(), "IP/DNS cannot be blank.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Log.d("newpacket", DataStorage.FILE_LINE(savePacket + ""));
                    NewPacketActivity NPA = (NewPacketActivity)getActivity();
                    NPA.dataStore.savePacket(savePacket);
                    NPA.dataStore.invalidateLists();
                    NPA.finish();

                }
            });

            return rootView;
        }
    }

}
