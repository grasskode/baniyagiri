package com.grasskode.baniyagiri.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.adapters.AggregatedExpenseAdapter;
import com.grasskode.baniyagiri.adapters.ExpenseAdapter;
import com.grasskode.baniyagiri.adapters.TagsListAdapter;
import com.grasskode.baniyagiri.dao.ExpenseManager;
import com.grasskode.baniyagiri.dao.TagsManager;
import com.grasskode.baniyagiri.elements.Aggregation;
import com.grasskode.baniyagiri.elements.Expense;
import com.grasskode.baniyagiri.elements.Group;
import com.grasskode.baniyagiri.layouts.FlowLayout;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for displaying reports on the expenses.
 *
 * Created by karan on 18/5/16.
 */
public class ReportActivity extends ToastResultActivity {

    static final String DATE_FORMAT = "%02d/%02d/%d";
    static final String SDF_FORMAT = "dd/MM/yyyy";

    List<String> tags;
    List<String> negativeTags;
    List<Expense> expenses;
    ExpenseManager manager;
    TagsManager tagsManager;

    TextView dateFromView;
    TextView clearDateFromView;
    TextView dateToView;
    TextView clearDateToView;
    Spinner aggregationsSpinner;
    AutoCompleteTextView tagsAutoCompleteView;
    FlowLayout tagsContainer;
    LinearLayout totalLayout;
    LinearLayout totalContainer;
    TextView emptyListView;
    ListView listView;

    LayoutInflater inflater;

    BaseAdapter adapter;

    AlertDialog.Builder destDialog;
    EditText destInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get views
        dateFromView = (TextView) findViewById(R.id.config_date_from);
        clearDateFromView = (TextView) findViewById(R.id.config_date_from_x);
        dateToView = (TextView) findViewById(R.id.config_date_to);
        clearDateToView = (TextView) findViewById(R.id.config_date_to_x);
        aggregationsSpinner = (Spinner) findViewById(R.id.aggregations_spinner);
        tagsAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.ac_tags);
        tagsContainer = (FlowLayout) findViewById(R.id.tags_container);
        totalLayout = (LinearLayout) findViewById(R.id.total_layout);
        totalContainer = (LinearLayout) findViewById(R.id.total_container);
        emptyListView = (TextView) findViewById(R.id.empty_list);
        listView = (ListView) findViewById(R.id.expense_list);

        // inflater
        inflater = (LayoutInflater) getApplicationContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // destination dialog box
        destDialog = new AlertDialog.Builder(this);
        destDialog.setTitle("Choose a file name");
        destInput = new EditText(this);
        destInput.setInputType(InputType.TYPE_CLASS_TEXT);
        destDialog.setView(destInput);
        // Set up the buttons
        destDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exportExpenses(destInput.getText().toString());
            }
        });
        destDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // initialize
        tags = new ArrayList<>();
        negativeTags = new ArrayList<>();
        expenses = new ArrayList<>();

        // initialize expense manager
        manager = new ExpenseManager(this);
        tagsManager = new TagsManager(this);

        // set up autocomplete tags list
        TagsManager tagsManager = new TagsManager(this);
        final TagsListAdapter tagsListAdapter = new TagsListAdapter(this,
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1,
                tagsManager.getAllTags());
        tagsAutoCompleteView.setAdapter(tagsListAdapter);
        tagsAutoCompleteView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tag = tagsListAdapter.getItem(position);
                if (!tags.contains(tag))
                    addTag(tag);
                tagsAutoCompleteView.setText("");
            }
        });

        // set up aggregations spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.aggregations_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aggregationsSpinner.setAdapter(adapter);
        aggregationsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                displayAggregatedData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ReportActivity.this, EditExpenseActivity.class);
                i.putExtra("tags", tags.toArray(new String[tags.size()]));
                startActivityForResult(i, REQ_CODE_EDIT_EXPENSE);
            }
        });

        // get expense_id from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                String[] extraTags = extras.getStringArray("tags");
                for(String tag : extraTags) {
                    if(!tags.contains(tag))
                        addTag(tag);
                }
            } catch (NullPointerException e) {
                // tags not present
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // create report for the given parameters
        createReport();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.report_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_export_expenses :
                // ask for destination and export
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HHmm");
                destInput.setText(String.format("expenses-%s", sdfDate.format(new Date())));
                destDialog.show();
                break;
            default:
                Toast.makeText(this, getResources().getString(R.string.unknown_action), Toast.LENGTH_SHORT)
                        .show();
        }

        return super.onOptionsItemSelected(item);
    }

    public void displayDatePicker(View view) {
        Calendar cal = Calendar.getInstance();
        long datetime = System.currentTimeMillis();
        cal.setTimeInMillis(datetime);
        switch (view.getId()) {
            case R.id.config_date_from:
                DatePickerDialog.OnDateSetListener dateFromSetListener = new DatePickerDialog.OnDateSetListener(){
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dateFromView.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, year));
                        clearDateFromView.setVisibility(View.VISIBLE);
                        createReport();
                    }
                };
                new DatePickerDialog(ReportActivity.this, dateFromSetListener,
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        .show();
                break;
            case R.id.config_date_to:
                DatePickerDialog.OnDateSetListener dateToSetListener = new DatePickerDialog.OnDateSetListener(){
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dateToView.setText(String.format(DATE_FORMAT, dayOfMonth, monthOfYear + 1, year));
                        clearDateToView.setVisibility(View.VISIBLE);
                        createReport();
                    }
                };
                new DatePickerDialog(ReportActivity.this, dateToSetListener,
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        .show();
                break;
        }
    }

    private void addTag(String tag) {
        if(tag.equals(Group.DEFAULT_TAG))
           return;

        tag = tag.trim();
        tags.add(tag);
        // add tag view to the tags container
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(0, 0, 3, 3);
        View view = inflater.inflate(R.layout.view_tag_editable, null);
        view.setLayoutParams(lparams);
        TextView tagNameView = (TextView) view.findViewById(R.id.tag_name);
        tagNameView.setText(tag);
        tagNameView.setClickable(true);
        tagNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = (View) v.getParent();
                String tag = ((TextView) view.findViewById(R.id.tag_name)).getText().toString();
                if (tags.contains(tag)) {
                    view.setBackgroundResource(R.color.colorTagNegative);
                    tags.remove(tag);
                    negativeTags.add(tag);
                } else {
                    view.setBackgroundResource(R.drawable.tags_border);
                    negativeTags.remove(tag);
                    tags.add(tag);
                }
                createReport();
            }
        });
        TextView xTextView = (TextView) view.findViewById(R.id.x_remove);
        xTextView.setClickable(true);
        xTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = (View) v.getParent();
                String tag = ((TextView) view.findViewById(R.id.tag_name)).getText().toString();
                if (tags.contains(tag)) {
                    tags.remove(tag);
                } else {
                    negativeTags.remove(tag);
                }
                tagsContainer.removeView(view);
                createReport();
            }
        });
        tagsContainer.addView(view);
        createReport();
    }

    private void addTotal(String curr, float amount) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lparams.setMargins(0, 0, 3, 3);
        View view = inflater.inflate(R.layout.view_report_curr_total, null);
        view.setLayoutParams(lparams);
        TextView currTV = (TextView) view.findViewById(R.id.expense_currency);
        currTV.setText(curr);
        TextView amountTV = (TextView) view.findViewById(R.id.expense_amount);
        amountTV.setText(String.valueOf(amount));
        totalContainer.addView(view);
    }

    private long parseAndGetStartDatetime() {
        String dateStr = dateFromView.getText().toString();
        if (dateStr.isEmpty())
            return 0;

        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT, Locale.getDefault());
        long datetime = 0;
        try {
            datetime = sdf.parse(String.format("%s", dateStr)).getTime();
        } catch (ParseException e) {
            Log.e(ReportActivity.class.getSimpleName(), "Error parsing datetime.", e);
        }

        return datetime;
    }

    private long parseAndGetEndDatetime() {
        String dateStr = dateToView.getText().toString();
        if (dateStr.isEmpty())
            return 0;

        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT, Locale.getDefault());
        long datetime = 0;
        try {
            datetime = sdf.parse(String.format("%s", dateStr)).getTime();
        } catch (ParseException e) {
            Log.e(ReportActivity.class.getSimpleName(), "Error parsing datetime.", e);
        }

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(datetime);
        cal.add(Calendar.DATE, 1);
        return cal.getTimeInMillis();
    }

    private Aggregation getAggregation(String agg) {
        switch (agg.toLowerCase()) {
            case "yearly" :
                return Aggregation.YEARLY;
            case "monthly" :
            return Aggregation.MONTHLY;
            case "weekly" :
                return Aggregation.WEEKLY;
            case "daily" :
                return Aggregation.DAILY;
            default:
                return Aggregation.NONE;
        }
    }

    private void displayExpenseTotals() {
        totalContainer.removeAllViews();
        Map<String, Float> totals = new HashMap<>();
        for (Expense ex : expenses) {
            float temp = ex.getAmount();
            if(totals.containsKey(ex.getCurrency())) {
                temp += totals.get(ex.getCurrency());
            }
            totals.put(ex.getCurrency(), temp);
        }

        List<Map.Entry<String, Float>> list = new ArrayList<>( totals.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        for(Map.Entry<String, Float> entry : list) {
            addTotal(entry.getKey(), entry.getValue());
        }
    }

    private void displayAggregatedData() {
        Aggregation aggregation = getAggregation((String) aggregationsSpinner.getSelectedItem());
        if(aggregation == Aggregation.NONE) {
            adapter = new ExpenseAdapter(this, expenses);
        } else {
            adapter = new AggregatedExpenseAdapter(this, expenses);
            ((AggregatedExpenseAdapter)adapter).aggregateFor(aggregation);
        }
        adapter.notifyDataSetChanged();
        if(expenses.size() == 0) {
            emptyListView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            totalLayout.setVisibility(View.GONE);
        } else {
            emptyListView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            totalLayout.setVisibility(View.VISIBLE);
        }
        listView.setAdapter(adapter);

        if(aggregation == Aggregation.NONE) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Expense expense = (Expense) adapter.getItem(position);
                    Intent i = new Intent(ReportActivity.this, EditExpenseActivity.class);
                    i.putExtra("expense_id", expense.getId());
                    startActivity(i);
                }
            });
        } else {
            listView.setOnItemClickListener(null);
        }
    }

    private void createReport() {
        // get expenses
        long start = parseAndGetStartDatetime();
        long end = parseAndGetEndDatetime();

        if(start > 0 && end > 0 && start > end) {
            Toast.makeText(this, getString(R.string.error_date_range), Toast.LENGTH_LONG)
                    .show();
        }

        expenses = manager.getExpenses(start, end, tags, negativeTags);

        // display totals
        displayExpenseTotals();

        // display aggregations
        displayAggregatedData();
    }

    public void clearDateFrom(View view) {
        if(dateFromView.getText().length() > 0) {
            dateFromView.setText("");
            dateFromView.setHint(R.string.beginning_of_time);
            clearDateFromView.setVisibility(View.GONE);
            createReport();
        }
    }

    public void clearDateTo(View view) {
        if(dateToView.getText().length() > 0){
            dateToView.setText("");
            dateToView.setHint(R.string.end_of_time);
            clearDateToView.setVisibility(View.GONE);
            createReport();
        }
    }

    public void exportExpenses(String destination) {
        // check if external storage is writable
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // finalize file name
            int count = 0;
            String ext = ".csv";

            File destFolder = new File(Environment.getExternalStorageDirectory(),
                    getString(R.string.app_name).toLowerCase());
            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }

            File dest = new File(destFolder, destination+ext);
            while(dest.exists()) {
                count += 1;
                dest = new File(destFolder, destination+"_"+count+ext);
            }
            try{
                dest.createNewFile();
                CSVWriter writer = new CSVWriter(new FileWriter(dest));
                for(Expense e : expenses) {
                    e.setTags(tagsManager.getExpenseTags(e.getId()));
                    writer.writeNext(e.toCSVRow());
                }
                writer.close();
                Toast.makeText(this, String.format(getString(R.string.export_completed), dest.getName()),
                        Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(ReportActivity.class.getSimpleName(), "Error exporting expenses.", e);
                Toast.makeText(this, getString(R.string.error_export), Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            Toast.makeText(this, getString(R.string.error_external_storage), Toast.LENGTH_LONG)
                    .show();
        }
    }

}
