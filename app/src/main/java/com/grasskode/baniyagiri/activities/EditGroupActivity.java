package com.grasskode.baniyagiri.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
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
import com.grasskode.baniyagiri.dao.GroupManager;
import com.grasskode.baniyagiri.dao.TagsManager;
import com.grasskode.baniyagiri.elements.Group;
import com.grasskode.baniyagiri.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for editing or creating groups.
 *
 * Created by karan on 29/7/16.
 */
public class EditGroupActivity extends AppCompatActivity {

    private static final int NAME_MIN_CHARS = 3;

    EditText nameView;
    EditText descriptionView;
    Button createButton;
    Button deleteButton;
    AutoCompleteTextView tagsAutoCompleteView;
    FlowLayout tagsContainer;
    LayoutInflater inflater;

    List<String> tags;
    Group group;

    GroupManager groupManager;
    TagsManager tagsManager;

    boolean isDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get views
        nameView = (EditText) findViewById(R.id.group_name);
        descriptionView = (EditText) findViewById(R.id.group_description);
        tagsAutoCompleteView = (AutoCompleteTextView) findViewById(R.id.ac_tags);
        tagsContainer = (FlowLayout) findViewById(R.id.tags_container);
        createButton = (Button) findViewById(R.id.create_btn);
        deleteButton = (Button) findViewById(R.id.delete_btn);

        // inflater
        inflater = (LayoutInflater) getApplicationContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // initialize
        tags = new ArrayList<>();
        isDefault = false;

        // initialize manager
        groupManager = new GroupManager(this);
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        // get expense_id from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                int group_id = extras.getInt("group_id", -1);
                group = groupManager.getGroup(group_id);
            } catch (NullPointerException e) {
                // group ID not present
            }
        }

        if (group != null) {
            // fill in edit text with data
            nameView.setText(group.getName());
            descriptionView.setText(group.getDescription());

            for (String tag : group.getTags()) {
                if (!tags.contains(tag))
                    addTag(tag);
            }
            createButton.setText(getString(R.string.edit));
            deleteButton.setVisibility(View.VISIBLE);

            if(group.getName().equals(Group.DEFAULT_NAME)) {
                isDefault = true;
                tagsAutoCompleteView.setVisibility(View.GONE);
                createButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                nameView.setEnabled(false);
                nameView.setTextColor(getResources().getColor(R.color.textPrimary));
                descriptionView.setEnabled(false);
                descriptionView.setTextColor(getResources().getColor(R.color.textPrimary));
            }
        }

    }

    private void addTag(String tag) {
        tag = tag.trim();

        if(tag.equals(Group.DEFAULT_TAG))
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
        tagNameView.setClickable(true);
        tagNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = (View) v.getParent();
                String tag = ((TextView) view.findViewById(R.id.tag_name)).getText().toString();
                if (tags.contains(tag)) {
                    view.setBackgroundResource(R.color.colorTagNegative);
                    tags.remove(tag);
                } else {
                    view.setBackgroundResource(R.drawable.tags_border);
                    tags.add(tag);
                }
            }
        });

        TextView xTextView = (TextView) view.findViewById(R.id.x_remove);
        xTextView.setClickable(true);
        xTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = (View) v.getParent();
                String tag = ((TextView) view.findViewById(R.id.tag_name)).getText().toString();
                if (tags.contains(tag))
                    tags.remove(tag);
                tagsContainer.removeView(view);
            }
        });
        if(isDefault) {
            xTextView.setVisibility(View.GONE);
        }
        tagsContainer.addView(view);
    }

    public void editGroup(View view) {
        Log.i("EDIT_GROUP", "Editing group.");
        Editable ed = nameView.getText();
        if (ed.length() < NAME_MIN_CHARS) {
            // too short
            Toast.makeText(this, R.string.name_too_short, Toast.LENGTH_SHORT)
                    .show();
        } else if (tags.isEmpty()) {
            // no tags
            Toast.makeText(this, R.string.group_no_tags, Toast.LENGTH_LONG)
                    .show();
        } else {
            // edit group
            String name = ed.toString();
            String description = descriptionView.getText().toString();

            try {
                if (group == null) {
                    group = new Group();
                }
                group.setName(name);
                group.setDescription(description);
                group.setTags(tags);
                group = groupManager.editGroup(group);
                // return to manage groups
                finish();
            } catch (Exception e) {
                Log.e("EDIT_GROUP", "Error editing group.", e);
                // return result to main activity
                Intent data = new Intent();
                data.putExtra("toast", getString(R.string.error_editing_group));
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }

    public void deleteGroup(View view) {
        if (group != null) {
            Log.i("DELETE_GROUP", "Delete group.");
            try {
                groupManager.deleteGroup(group.getId());
                // return to manage groups
                finish();
            } catch (Exception e) {
                Log.e("DELETE_GROUP", "Error deleting group.", e);
                // return result to main activity
                Intent data = new Intent();
                data.putExtra("toast", getString(R.string.error_deleting_group));
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }
}
