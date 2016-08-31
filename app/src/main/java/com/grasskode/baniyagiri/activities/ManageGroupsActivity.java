package com.grasskode.baniyagiri.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.adapters.GroupsAdapter;
import com.grasskode.baniyagiri.adapters.ManageGroupsAdapter;
import com.grasskode.baniyagiri.dao.GroupManager;
import com.grasskode.baniyagiri.elements.Group;

import java.util.List;

/**
 * Manage groups. Lists all groups.
 * Edit, delete or add new groups.
 *
 * Created by karan on 29/7/16.
 */
public class ManageGroupsActivity extends ToastResultActivity {

    ManageGroupsAdapter adapter;
    GroupManager manager;
    TextView emptyTextView;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set empty list text
        emptyTextView = (TextView) findViewById(R.id.empty_list);

        // initialize manager
        manager = new GroupManager(this);

        // prepare trips list view
        listView = (ListView) findViewById(R.id.groups_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = (Group) adapter.getItem(position);
                Intent i = new Intent(ManageGroupsActivity.this, EditGroupActivity.class);
                i.putExtra("group_id", group.getId());
                startActivityForResult(i, REQ_CODE_EDIT_GROUP);
            }
        });

        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ManageGroupsActivity.this, EditGroupActivity.class);
                startActivityForResult(i, REQ_CODE_EDIT_GROUP);
            }
        });

        // turn of groups message
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        if(sharedPref.getBoolean(SettingsActivity.SHOW_GROUPS_MSG, true)) {
            sharedPref.edit().putBoolean(SettingsActivity.SHOW_GROUPS_MSG, false).apply();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // load groups list
        List<Group> groupList = manager.getAllNonDefaultGroups();
        adapter = new ManageGroupsAdapter(this, groupList);
        adapter.notifyDataSetChanged();
        if(adapter.getCount() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
        listView.setAdapter(adapter);
    }

}