package com.grasskode.baniyagiri.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.grasskode.baniyagiri.elements.Expense;
import com.grasskode.baniyagiri.elements.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manage group CRUD operations.
 *
 * Created by karan on 28/7/16.
 */
public class GroupManager {

    Context context;
    DatabaseHelper dbHelper;

    ExpenseManager expenseManager;

    static final String[] groups_all_columns = {
            DataContract.GroupsEntry._ID,
            DataContract.GroupsEntry.COLUMN_NAME_NAME,
            DataContract.GroupsEntry.COLUMN_NAME_DESCRIPTION,
            DataContract.GroupsEntry.COLUMN_NAME_TAGS,
            DataContract.GroupsEntry.COLUMN_NAME_TIMESTAMP
    };
    
    public GroupManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        expenseManager = new ExpenseManager(context);
    }

    public Group editGroup(Group group) throws Exception {
        ContentValues values = new ContentValues();
        values.put(DataContract.GroupsEntry.COLUMN_NAME_NAME, group.getName());
        values.put(DataContract.GroupsEntry.COLUMN_NAME_DESCRIPTION, group.getDescription());
        values.put(DataContract.GroupsEntry.COLUMN_NAME_TAGS, TextUtils.join(",", group.getTags()));

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if(group.getId() > 0) {
                // update
                Log.d(GroupManager.class.getSimpleName(), String.format("Updating group %d.", group.getId()));
                db.update(DataContract.GroupsEntry.TABLE_NAME, values,
                        DataContract.GroupsEntry._ID + " = " + group.getId(), null);
                Log.d(GroupManager.class.getSimpleName(), String.format("Edited group with ID %d.", group.getId()));
            } else {
                // create
                Log.d(GroupManager.class.getSimpleName(), "Creating new group.");
                values.put(DataContract.GroupsEntry.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());
                long groupId = db.insert(DataContract.GroupsEntry.TABLE_NAME, "null", values);
                if(groupId == -1)
                    throw new Exception("Error creating new group");
                else {
                    Log.d(GroupManager.class.getSimpleName(), String.format("Created group with ID %d.", groupId));
                    group.setId((int) groupId);
                }
            }
        } finally {
            db.close();
        }

        // return group
        return group;
    }

    public Group getGroup(int groupId) {
        Log.d(GroupManager.class.getSimpleName(), String.format("Getting group with ID %d.", groupId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.GroupsEntry.TABLE_NAME, groups_all_columns,
                DataContract.GroupsEntry._ID + " = " + groupId, null, null, null,
                null);
        Group group = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            group = cursorToGroup(cursor);
        }
        cursor.close();
        db.close();

        List<Integer> eIds = expenseManager.getExpenseIds(0, 0, group.getTags(), null);
        if(eIds.size() > 0) {
            Expense e = expenseManager.getExpense(eIds.get(0));
            group.setEndDatetime(e.getDatetime());

            e = expenseManager.getExpense(eIds.get(eIds.size()-1));
            group.setStartDatetime(e.getDatetime());
        }

        return group;
    }

    public boolean deleteGroup(int groupId) {
        Log.d(GroupManager.class.getSimpleName(), String.format("Deleting group with ID %d.", groupId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int affected = db.delete(DataContract.GroupsEntry.TABLE_NAME,
                DataContract.GroupsEntry._ID + " = " + groupId, null);
        db.close();

        return affected > 0;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();

        Log.d(GroupManager.class.getSimpleName(), "Getting all groups.");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.GroupsEntry.TABLE_NAME,
                groups_all_columns, null, null, null, null,
                DataContract.GroupsEntry._ID + " ASC"); // order by
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            groups.add(cursorToGroup(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        db.close();

        for(Group g : groups) {
            List<Integer> eIds = expenseManager.getExpenseIds(0, 0, g.getTags(), null);
            if(eIds.size() > 0) {
                Expense e = expenseManager.getExpense(eIds.get(0));
                g.setEndDatetime(e.getDatetime());

                e = expenseManager.getExpense(eIds.get(eIds.size()-1));
                g.setStartDatetime(e.getDatetime());
            }
        }

        // sort by end date
        Collections.sort(groups, new Comparator<Group>() {
            @Override
            public int compare(Group lhs, Group rhs) {
                // reverse sort
                long diff = rhs.getEndDatetime() - lhs.getEndDatetime();
                if (diff < 0)
                    return -1;
                else if (diff > 0)
                    return 1;
                else return 0;
            }
        });
        return groups;
    }

    public List<Group> getAllNonDefaultGroups() {
        List<Group> groups = new ArrayList<>();

        Log.d(GroupManager.class.getSimpleName(), "Getting all non default groups.");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.GroupsEntry.TABLE_NAME,
                groups_all_columns,
                DataContract.GroupsEntry.COLUMN_NAME_NAME +" != ?",         // selection
                new String[]{Group.DEFAULT_NAME},                           // selection args
                null, null,
                DataContract.GroupsEntry.COLUMN_NAME_TIMESTAMP + " DESC");  // order by
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            groups.add(cursorToGroup(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        db.close();

        for(Group g : groups) {
            List<Integer> eIds = expenseManager.getExpenseIds(0, 0, g.getTags(), null);
            if(eIds.size() > 0) {
                Expense e = expenseManager.getExpense(eIds.get(0));
                g.setEndDatetime(e.getDatetime());

                e = expenseManager.getExpense(eIds.get(eIds.size()-1));
                g.setStartDatetime(e.getDatetime());
            }
        }

        return groups;
    }

    private Group cursorToGroup(Cursor cursor) {
        Group g = new Group();
        g.setId(cursor.getInt(0));
        g.setName(cursor.getString(1));
        g.setDescription(cursor.getString(2));
        for(String tag : cursor.getString(3).split(",")) {
            g.addTag(tag);
        }
        g.setTimestamp(cursor.getLong(4));
        return g;
    }

}
