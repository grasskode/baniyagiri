package com.grasskode.baniyagiri.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.adapters.TagsListAdapter;
import com.grasskode.baniyagiri.dao.ExpenseManager;
import com.grasskode.baniyagiri.dao.TagsManager;
import com.grasskode.baniyagiri.elements.Expense;
import com.grasskode.baniyagiri.elements.Group;
import com.grasskode.baniyagiri.layouts.FlowLayout;
import com.grasskode.baniyagiri.services.FetchAddressIntentService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Activity to edit (create or update) an expense
 *
 * Created by karan on 18/5/16.
 */
public class EditExpenseActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int TAG_MIN_CHARS = 2;
    private static final int NAME_MIN_CHARS = 3;

    static final String TIME_FORMAT = "%02d:%02d";
    static final String DATE_FORMAT = "%02d/%02d/%d";

    EditText nameView;
    TextView dateView;
    TextView timeView;
    EditText amountView;
    Button createButton;
    Button deleteButton;
    AutoCompleteTextView tagsAutoCompleteView;
    FlowLayout tagsContainer;
    LayoutInflater inflater;

    Spinner currencyView;
    ArrayAdapter<String> currAdapter;

    Expense expense;
    List<String> tags;
    ExpenseManager expenseManager;
    TagsManager tagsManager;

    List<String> locationTags;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    AddressResultReceiver locationResultReceiver;

    boolean countryTagging;
    boolean cityTagging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_expense);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get views
        nameView = (EditText) findViewById(R.id.expense_name);
        dateView = (TextView) findViewById(R.id.expense_date);
        timeView = (TextView) findViewById(R.id.expense_time);
        amountView = (EditText) findViewById(R.id.expense_amount);
        tagsAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.add_tag);
        tagsContainer = (FlowLayout) findViewById(R.id.tags_container);
        createButton = (Button) findViewById(R.id.create_btn);
        deleteButton = (Button) findViewById(R.id.delete_btn);

        // populate currencies
        currencyView = (Spinner) findViewById(R.id.expense_currency);
        Set<Currency> currencies = Currency.getAvailableCurrencies();
        List<String> currCodes = new ArrayList<>(currencies.size());
        for (Currency currency:currencies) {
            currCodes.add(currency.getCurrencyCode());
        }
        Collections.sort(currCodes);
        currAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currCodes);
        currencyView.setAdapter(currAdapter);

        // inflater
        inflater = (LayoutInflater) getApplicationContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // set the dates and times
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        dateView.setText(String.format(DATE_FORMAT,
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));
        timeView.setText(String.format(TIME_FORMAT,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

        // initialize tags
        tags = new ArrayList<>();
        locationTags = null;

        // initialize google api client
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // initialize manager
        expenseManager = new ExpenseManager(this);
        tagsManager = new TagsManager(this);

        // set up autocomplete tags list
        TagsManager tagsManager = new TagsManager(this);
        final TagsListAdapter tagsListAdapter = new TagsListAdapter(this,
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1,
                tagsManager.getAllTags());
        tagsAutoCompleteView.setAdapter(tagsListAdapter);
        tagsAutoCompleteView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            addTag(v);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        tagsAutoCompleteView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tag = tagsListAdapter.getItem(position);
                addTag(tag);
                tagsAutoCompleteView.setText("");
            }
        });

        // location tagging
        countryTagging = false;
        cityTagging = false;

        setExpense();
    }

    private void setExpense() {
        // get expense_id from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                int expense_id = extras.getInt("expense_id", -1);
                expense = expenseManager.getExpense(expense_id);
                expense.setTags(tagsManager.getExpenseTags(expense_id));
            } catch (NullPointerException e) {
                // expense ID not present
            }

            try {
                String[] extraTags = extras.getStringArray("tags");
                if(extraTags != null) {
                    for(String tag : extraTags) {
                        addTag(tag);
                    }
                }
            } catch (NullPointerException e) {
                // tags not present
            }
        }

        if (expense != null) {
            // fill in edit text with data
            nameView.setText(expense.getName());
            amountView.setText(String.valueOf(expense.getAmount()));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(expense.getDatetime());
            dateView.setText(String.format(DATE_FORMAT, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));
            timeView.setText(String.format(TIME_FORMAT, cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)));

            createButton.setText(getString(R.string.edit));
            deleteButton.setVisibility(View.VISIBLE);

            currencyView.setSelection(currAdapter.getPosition(expense.getCurrency()));


            for (String tag : expense.getTags())
                addTag(tag);
        } else {
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE);
            // look up currency code in shared preferences
            String code = sharedPref.getString(SettingsActivity.PREFERRED_CURRENCY, null);
            if (code == null) {
                // code by current locale
                code = Currency.getInstance(getResources().getConfiguration().locale).getCurrencyCode();
                sharedPref.edit().putString(SettingsActivity.PREFERRED_CURRENCY, code).apply();
            }
            currencyView.setSelection(currAdapter.getPosition(code));

            countryTagging = sharedPref.getBoolean(SettingsActivity.COUNTRY_TAGGING, false);
            cityTagging = sharedPref.getBoolean(SettingsActivity.CITY_TAGGING, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationResultReceiver = null;
        if(expense == null && (countryTagging || cityTagging) && locationTags == null) {
            // get location tags
            googleApiClient.connect();

            // register broadcast receiver
            IntentFilter filter = new IntentFilter(AddressResultReceiver.ACTION_RESP);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            locationResultReceiver = new AddressResultReceiver();
            registerReceiver(locationResultReceiver, filter);
        }
    }

    public void displayDatetimePicker(View view) {
        Calendar cal = Calendar.getInstance();
        long datetime = parseAndGetDatetime();
        switch (view.getId()) {
            case R.id.expense_date:
                cal.setTimeInMillis(datetime);
                DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dateView.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, year));
                    }
                };
                new DatePickerDialog(EditExpenseActivity.this, dateSetListener,
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        .show();
                break;
            case R.id.expense_time:
                cal.setTimeInMillis(datetime);
                TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        timeView.setText(String.format(TIME_FORMAT, hourOfDay, minute));
                    }
                };
                new TimePickerDialog(EditExpenseActivity.this, timeSetListener,
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                        .show();
                break;
        }
    }

    private long parseAndGetDatetime() {
        String dateStr = dateView.getText().toString();
        String timeStr = timeView.getText().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        long datetime = 0;
        try {
            datetime = sdf.parse(String.format("%s %s", dateStr, timeStr)).getTime();
        } catch (ParseException e) {
            Log.e(EditExpenseActivity.class.getSimpleName(), "Error parsing datetime.", e);
        }

        return datetime;
    }

    private void addTag(String tag) {
        if(tag == null)
            return;

        tag = tag.trim().toLowerCase();

        if (tag.equals(Group.DEFAULT_TAG))
            return;
        if (tags.contains(tag))
            return;

        tags.add(tag);
        // add tag view to the tags container
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(0, 0, 3, 3);
        View view = inflater.inflate(R.layout.view_tag_editable, null);
        view.setLayoutParams(lparams);
        TextView tagNameView = (TextView) view.findViewById(R.id.tag_name);
        tagNameView.setText(tag);
        TextView xTextView = (TextView) view.findViewById(R.id.x_remove);
        xTextView.setClickable(true);
        xTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = (View) v.getParent();
                String tag = ((TextView) view.findViewById(R.id.tag_name)).getText().toString();
                Log.i("REMOVE_TAG", String.format("Removing tag %s.", tag));
                tags.remove(tag);
                tagsContainer.removeView(view);
            }
        });
        tagsContainer.addView(view);
    }

    public void addTag(View view) {
        EditText tagEdView = (EditText) findViewById(R.id.add_tag);
        Editable ed = tagEdView.getText();
        if (ed.length() < TAG_MIN_CHARS) {
            // too short
            Toast.makeText(this, R.string.tag_too_short, Toast.LENGTH_SHORT)
                    .show();
        } else {
            // add tag
            String tag = ed.toString();
            if (!tags.contains(tag))
                addTag(tag);
            else
                Toast.makeText(this, R.string.tag_already_present, Toast.LENGTH_SHORT)
                        .show();
            // clear tag edit view
            tagEdView.setText("");
        }
    }

    public void editExpense(View view) {
        Log.i("EDIT_EXPENSE", "Editing expense.");
        EditText nameView = (EditText) findViewById(R.id.expense_name);
        Editable ed = nameView.getText();
        Editable amt = amountView.getText();
        if (ed.length() < NAME_MIN_CHARS) {
            // too short
            Toast.makeText(this, R.string.name_too_short, Toast.LENGTH_SHORT)
                    .show();
        } else if (amt.length() == 0) {
            // no amount
            Toast.makeText(this, R.string.add_amount, Toast.LENGTH_SHORT)
                    .show();
        } else {
            // edit expense
            String name = ed.toString();
            float amount = Float.valueOf(amt.toString());
            String currency = currAdapter.getItem(currencyView.getSelectedItemPosition());
            long datetime = parseAndGetDatetime();
            try {
                if (expense == null) {
                    expense = new Expense();
                }
                expense.setName(name);
                expense.setDatetime(datetime);
                expense.setAmount(amount);
                expense.setCurrency(currency);
                expense.setTags(tags);
                expense = expenseManager.editExpense(expense);
                // return to expense list
                finish();
            } catch (Exception e) {
                Log.e("ADD_EXPENSE", "Error adding expense.", e);
                // return result to main activity
                Intent data = new Intent();
                data.putExtra("toast", getString(R.string.error_adding_expense));
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }

    public void deleteExpense(View view) {
        if (expense != null) {
            Log.i("DELETE_EXPENSE", "Delete expense.");
            try {
                expenseManager.deleteExpense(expense.getId());
                // return to expense list
                finish();
            } catch (Exception e) {
                Log.e("DELETE_EXPENSE", "Error deleting expense.", e);
                // return result to main activity
                Intent data = new Intent();
                data.putExtra("toast", getString(R.string.error_deleting_expense));
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        if (locationResultReceiver != null) {
            unregisterReceiver(locationResultReceiver);
        }
        googleApiClient.disconnect();
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Gets the best and most recent location currently available,
        // which may be null in rare cases when a location is not available.
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);

        if (lastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                return;
            }

            startIntentService();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("API_CONN", "api connection failed");
        locationTags = null;
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra("location", lastLocation);
        startService(intent);
    }

    public class AddressResultReceiver extends BroadcastReceiver {

        public static final String ACTION_RESP =
                "com.grasskode.baniyagiri.LOCATION_TAGS";

        @Override
        public void onReceive(Context context, Intent intent) {
            String[] result = intent.getStringArrayExtra("result");

            if(result.length > 0 && locationTags == null) {
                locationTags = new ArrayList<>();
                String cityTag = result[0];
                if(cityTagging && cityTag != null) {
                    locationTags.add(cityTag);
                    addTag(cityTag);
                }
                String countryTag = result[1];
                if(countryTagging && countryTag != null) {
                    locationTags.add(countryTag);
                    addTag(countryTag);
                }
            }
        }
    }
}
