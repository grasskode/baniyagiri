package com.grasskode.baniyagiri.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import com.grasskode.baniyagiri.R;

import java.util.Currency;

/**
 * Created by karan on 30/7/16.
 */
public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // load shared preferences
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        if(!sharedPref.getBoolean(SettingsActivity.INITIALIZED, false)) {
            // initialize with default settings
            SharedPreferences.Editor editor = sharedPref.edit();
            String code = Currency.getInstance(getResources().getConfiguration().locale).getCurrencyCode();
            editor.putString(SettingsActivity.PREFERRED_CURRENCY, code);
            editor.putBoolean(SettingsActivity.COUNTRY_TAGGING, true);
            editor.putBoolean(SettingsActivity.ADMIN_TAGGING, true);
            editor.putBoolean(SettingsActivity.LOCALITY_TAGGING, true);
            editor.putBoolean(SettingsActivity.INITIALIZED, true);
            editor.apply();
        }

        // start trips activity after delay
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startApplication();
            }
        }, 2000);
    }

    private void startApplication() {
        // start the trips activity
        Intent i = new Intent(LauncherActivity.this, GroupsActivity.class);
        startActivity(i);
    }
}