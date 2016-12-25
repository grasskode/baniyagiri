package com.grasskode.baniyagiri.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.adapters.GroupsAdapter;
import com.grasskode.baniyagiri.dao.GroupManager;
import com.grasskode.baniyagiri.elements.Group;

import java.util.List;

/**
 * Activity to list groups.
 *
 * Created by karan on 27/7/16.
 */
public class GroupsActivity extends ToastResultActivity {

    GroupsAdapter adapter;
    GroupManager manager;
    TextView emptyTextView;
    ListView listView;
    TextView groupsMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);
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
                Intent i = new Intent(GroupsActivity.this, ReportActivity.class);
                i.putExtra("tags", group.getTags().toArray(new String[group.getTags().size()]));
                startActivityForResult(i, REQ_CODE_ANALYTICS);
            }
        });

        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(GroupsActivity.this, EditExpenseActivity.class);
                startActivityForResult(i, REQ_CODE_EDIT_EXPENSE);
            }
        });

        // groups message view
        groupsMessageView = (TextView) findViewById(R.id.groups_message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings :
                Intent i = new Intent(GroupsActivity.this, SettingsActivity.class);
                startActivityForResult(i, REQ_CODE_SETTINGS);
                return true;
            case R.id.action_manage_groups:
                i = new Intent(GroupsActivity.this, ManageGroupsActivity.class);
                startActivityForResult(i, REQ_CODE_MANAGE_GROUPS);
                return true;
            case R.id.action_import_expenses:
                i = new Intent(GroupsActivity.this, ImportExpensesActivity.class);
                startActivityForResult(i, REQ_CODE_IMPORT);
                return true;
            default:
                Toast.makeText(this, getResources().getString(R.string.unknown_action), Toast.LENGTH_SHORT)
                        .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // load groups list
        List<Group> groupList = manager.getAllGroups();
        adapter = new GroupsAdapter(this, groupList);
        adapter.notifyDataSetChanged();
        if(adapter.getCount() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
        listView.setAdapter(adapter);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        if(sharedPref.getBoolean(SettingsActivity.SHOW_GROUPS_MSG, true)) {
            groupsMessageView.setVisibility(View.VISIBLE);
        } else {
            groupsMessageView.setVisibility(View.GONE);
        }
    }
}