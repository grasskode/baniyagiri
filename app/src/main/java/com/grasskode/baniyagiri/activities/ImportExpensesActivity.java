package com.grasskode.baniyagiri.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.adapters.TagsListAdapter;
import com.grasskode.baniyagiri.dao.ExpenseManager;
import com.grasskode.baniyagiri.dao.TagsManager;
import com.grasskode.baniyagiri.elements.Expense;
import com.grasskode.baniyagiri.elements.Group;
import com.grasskode.baniyagiri.layouts.FlowLayout;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by karan on 25/12/16.
 */
public class ImportExpensesActivity extends ToastResultActivity {

    private static final int FILE_SELECT_CODE = 0;

    private static final int TAG_MIN_CHARS = 2;

    File importFile;

    LinearLayout importDetails;
    TextView fileName;
    TextView importText;
    Button importButton;
    AutoCompleteTextView tagsAutoCompleteView;
    FlowLayout tagsContainer;
    LayoutInflater inflater;

    ExpenseManager expenseManager;
    TagsManager tagsManager;

    List<Expense> expenses;
    List<String> tags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // layouts and views
        importDetails = (LinearLayout) findViewById(R.id.import_details);
        fileName = (TextView) findViewById(R.id.import_file);
        importText = (TextView) findViewById(R.id.import_text);
        importButton = (Button) findViewById(R.id.import_btn);
        tagsAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.add_tag);
        tagsContainer = (FlowLayout) findViewById(R.id.tags_container);

        // inflater
        inflater = (LayoutInflater) getApplicationContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // initialize manager
        expenseManager = new ExpenseManager(this);
        tagsManager = new TagsManager(this);

        // initialize tags
        tags = new ArrayList<>();

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
    }

    @Override
    public void onResume() {
        super.onResume();

        if(importFile == null) {
            importDetails.setVisibility(View.GONE);
        } else {
            importDetails.setVisibility(View.VISIBLE);
        }
    }

    public void chooseFile(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select file"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No File Manager found. Install one!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    importFile = new File(data.getData().getPath());
                    testImport();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void testImport() {
        // validate file format and data
        try {
            CSVReader data = new CSVReader(new FileReader(importFile));
            String[] values = data.readNext();
            expenses = new ArrayList<>();
            while (values != null) {
                Expense e = Expense.fromCSVRow(values);
                if (e != null) {
                    expenses.add(e);
                }
                values = data.readNext();
            }
            data.close();

            fileName.setText(importFile.getName());
            importText.setText(String.format(getString(R.string.num_expenses), expenses.size()));
            if(expenses.size() > 0) {
                importButton.setEnabled(true);
                importButton.setBackgroundResource(R.color.colorPrimaryLight);
            }
        } catch (IOException e) {
            importText.setText(getString(R.string.error_file_read));
        }
    }

    public void importExpenses(View v) {
        for(Expense e : expenses) {
            for(String t : tags) {
                if(!e.getTags().contains(t)) {
                    e.addTag(t);
                }
            }
            expenseManager.editExpense(e);
        }
        Toast.makeText(ImportExpensesActivity.this, String.format(getString(R.string.imported), expenses.size()),
                Toast.LENGTH_SHORT).show();
        finish();
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

}
