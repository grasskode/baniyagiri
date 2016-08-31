package com.grasskode.baniyagiri.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.grasskode.baniyagiri.elements.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager class to provide dao for tags.
 *
 * Created by karan on 18/5/16.
 */
public class TagsManager {

    Context context;
    DatabaseHelper dbHelper;

    static final String[] expense_tags_all_columns = {
            DataContract.ExpenseTagsEntry._ID,
            DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE,
            DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG
    };

    public TagsManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public void addExpenseTags(int expenseId, List<String> tags) {
        Log.d(TagsManager.class.getSimpleName(), String.format("Creating tags for expense with ID %d.", expenseId));

        if(!tags.contains(Group.DEFAULT_TAG)) {
            tags.add(Group.DEFAULT_TAG);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            // remove all expense tags for given expense
            db.delete(DataContract.ExpenseTagsEntry.TABLE_NAME,
                    DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE + " = " + expenseId, null);

            // add all expense tags
            ContentValues values;
            for (String tag : tags) {
                values = new ContentValues();
                values.put(DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE, expenseId);
                values.put(DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG, tag);

                db.insert(DataContract.ExpenseTagsEntry.TABLE_NAME, "null", values);
            }

            // success of transaction
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TagsManager.class.getSimpleName(), "Error adding tags for expense.", e);
        } finally {
            db.endTransaction();
        }

        db.close();
        Log.d(TagsManager.class.getSimpleName(), String.format("Created %d expense tags.", tags.size()));
    }

    public List<String> getExpenseTags(int expenseId) {
        List<String> tags = new ArrayList<>();

        Log.d(TagsManager.class.getSimpleName(), String.format("Getting tags for expense with ID %d.", expenseId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.ExpenseTagsEntry.TABLE_NAME, expense_tags_all_columns,
                DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE + " = " + expenseId,
                null, null, null,
                DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG+" ASC");  // order by
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            tags.add(cursor.getString(2));
            cursor.moveToNext();
        }
        cursor.close();
        db.close();

        return tags;
    }

    public void deleteExpenseTags(int expenseId) {
        Log.d(TagsManager.class.getSimpleName(), String.format("Deleting tags for expense with ID %d.", expenseId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.delete(DataContract.ExpenseTagsEntry.TABLE_NAME,
                DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE + " = " + expenseId, null);
        db.close();
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();

        Log.d(TagsManager.class.getSimpleName(), "Getting all tags.");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true, DataContract.ExpenseTagsEntry.TABLE_NAME,
                new String[]{DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG},
                null, null,
                DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG, // group by
                null,
                DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG+" ASC", // order by
                null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            tags.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        db.close();

        tags.remove(Group.DEFAULT_TAG);
        return tags;
    }

}
