package com.grasskode.baniyagiri.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.grasskode.baniyagiri.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Activity to edit settings.
 *
 * Created by karan on 29/7/16.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String INITIALIZED = "initialized";
    public static final String PREFERRED_CURRENCY = "curr_code";
    public static final String COUNTRY_TAGGING = "tag_country";
    public static final String CITY_TAGGING = "tag_city";
    public static final String SHOW_GROUPS_MSG = "show_groups_message";

    AppCompatCheckBox countryCheckbox;
    AppCompatCheckBox cityCheckbox;

    Spinner currencyView;
    ArrayAdapter<String> currAdapter;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // load shared preferences
        sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        // get views
        countryCheckbox = (AppCompatCheckBox) findViewById(R.id.cb_country);
        cityCheckbox = (AppCompatCheckBox) findViewById(R.id.cb_city);

        // populate currencies
        currencyView = (Spinner) findViewById(R.id.currency);
        Set<Currency> currencies = Currency.getAvailableCurrencies();
        List<String> currCodes = new ArrayList<>(currencies.size());
        for (Currency currency:currencies) {
            currCodes.add(currency.getCurrencyCode());
        }
        Collections.sort(currCodes);
        currAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currCodes);
        currencyView.setAdapter(currAdapter);

        // look up currency code in shared preferences
        String code = sharedPref.getString(PREFERRED_CURRENCY, null);
        if (code == null) {
            // code by current locale
            code = Currency.getInstance(getResources().getConfiguration().locale).getCurrencyCode();
            if(code == null) {
                code = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
            }
            sharedPref.edit().putString(PREFERRED_CURRENCY, code).apply();
        }
        currencyView.setSelection(currAdapter.getPosition(code));

        currencyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // save to shared preferences
                sharedPref.edit().putString(PREFERRED_CURRENCY, currAdapter.getItem(position)).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        // set location tagging from preferences
        cityCheckbox.setChecked(sharedPref.getBoolean(CITY_TAGGING, false));
        countryCheckbox.setChecked(sharedPref.getBoolean(COUNTRY_TAGGING, false));
    }

    public void toggleLocationTagging(View view) {
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (view.getId()) {
            case R.id.cb_country:
                if(countryCheckbox.isChecked()) {
                    editor.putBoolean(COUNTRY_TAGGING, true);
                } else {
                    editor.putBoolean(COUNTRY_TAGGING, false);
                }
                editor.apply();
                break;
            case R.id.cb_city:
                if(cityCheckbox.isChecked()) {
                    editor.putBoolean(CITY_TAGGING, true);
                } else {
                    editor.putBoolean(CITY_TAGGING, false);
                }
                editor.apply();
            default:
                // do nothing
        }
    }

}
