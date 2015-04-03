//  Copyright (c) 2014 Dan Nagle. All rights reserved.
//
// Licensed MIT: https://github.com/dannagle/PacketSender-Android


package com.packetsender.android;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about, menu);
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

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_about, container, false);

            TextView aboutBlurb = (TextView) rootView.findViewById(R.id.aboutBlurb);
            aboutBlurb.setText(Html.fromHtml(getString(R.string.about_html)));

            TextView shopAmazonButton = (TextView) rootView.findViewById(R.id.shopAmazonButton);

            shopAmazonButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v)
                {
                    //go to dannagle.com/amazon
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://packetsender.com/"));
                    startActivity(browserIntent);
                }
            });

/*

            TextView packetsenderButton = (TextView) rootView.findViewById(R.id.packetsenderButton);
            packetsenderButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v)
                {
                    //go to dannagle.com/amazon

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://packetsender.com/"));
                    startActivity(browserIntent);
                }
            });
*/
            return rootView;
        }
    }

}
