package com.grasskode.baniyagiri.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.grasskode.baniyagiri.activities.EditExpenseActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by karan on 3/6/16.
 */
public class FetchAddressIntentService extends IntentService {

    public FetchAddressIntentService() {
        super(FetchAddressIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra("location");

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException e) {
            Log.e("FETCH_ADDRESS", "Unable to fetch Geocode.", e);
        } catch (IllegalArgumentException e) {
            Log.e("FETCH_ADDRESS", "Invalid arguments.", e);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            deliverResultToReceiver(null);
        } else {
            Address address = addresses.get(0);
            String town = address.getAdminArea();
            String country = address.getCountryName();

            Log.i("FETCH_ADDRESS", String.format("Found address -> %s, %s", town, country));
            deliverResultToReceiver(new String[]{town, country});
        }
    }

    private void deliverResultToReceiver(String[] addressFragments) {
        String[] resultArr = new String[]{};
        if(addressFragments != null) {
            resultArr = addressFragments;
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(EditExpenseActivity.AddressResultReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("result", resultArr);
        sendBroadcast(broadcastIntent);
    }

}
