package com.grasskode.baniyagiri.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.grasskode.baniyagiri.elements.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for DB creation and updates.
 *
 * Created by karan on 18/5/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 10;
    public static final String DATABASE_NAME = "Expensetag.db";

    public static final String TAG_DB_CREATE = "DB_CREATE";
    public static final String TAG_DB_UPGRADE = "DB_UPGRADE";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private int updateDBSchema(SQLiteDatabase db, int version) {
        Log.d(TAG_DB_UPGRADE, String.format("Updating DB schema from %d", version));
        switch (version) {
            case 5:
                Log.d(TAG_DB_UPGRADE, "Upgrading DB from version 5 to 6.");
                upgradeDB5(db);
                version = 6;
                break;
            case 6:
                Log.d(TAG_DB_UPGRADE, "Upgrading DB from version 6 to 7.");
                version = 7;
                createGroupsDefault(db);
                break;
            case 7:
                Log.d(TAG_DB_UPGRADE, "Upgrading DB from version 7 to 8.");
                version = 8;
                createGroupsDefault(db);
                break;
            case 8:
                Log.d(TAG_DB_UPGRADE, "Upgrading DB from version 8 to 9.");
                version = 9;
                createGroupsDefault(db);
                break;
            case 9:
                Log.d(TAG_DB_UPGRADE, "Upgrading DB from version 9 to 10.");
                Log.d(TAG_DB_UPGRADE, "No upgrade to perform.");
                version = 10;
                break;
            default:
                Log.d(TAG_DB_UPGRADE, String.format("Schema upgrade script not found for " +
                        "version %d! This might lead to errors.", version));
                version += 1;
                break;
        }
        return version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG_DB_CREATE, "Creating database. Any existing data will be discarded.");

        // recreate expenses table
        db.execSQL(DataContract.SQL_DELETE_EXPENSES);
        db.execSQL(DataContract.SQL_CREATE_EXPENSES);

        // recreate expense_tags table
        db.execSQL(DataContract.SQL_DELETE_EXPENSE_TAGS);
        db.execSQL(DataContract.SQL_CREATE_EXPENSE_TAGS);

        // recreate groups table
        createGroupsDefault(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG_DB_UPGRADE, String.format("Upgrading DB from %d to %d", oldVersion, newVersion));
        while(oldVersion < newVersion) {
            // update DB from oldVersion to newVersion
            oldVersion = updateDBSchema(db, oldVersion);
        }
    }

    private void upgradeDB5(SQLiteDatabase db) {
        Log.d(TAG_DB_UPGRADE, "Creating groups table.");
        db.execSQL(DataContract.SQL_DELETE_GROUPS);
        db.execSQL(DataContract.SQL_CREATE_GROUPS);

        Log.d(TAG_DB_UPGRADE, "Creating default group.");
        // create default group
        try {
            db.beginTransaction();

            Group d = Group.getDefaultGroup();

            ContentValues values;
            values = new ContentValues();
            values.put(DataContract.GroupsEntry.COLUMN_NAME_NAME, d.getName());
            values.put(DataContract.GroupsEntry.COLUMN_NAME_DESCRIPTION, d.getDescription());
            values.put(DataContract.GroupsEntry.COLUMN_NAME_TAGS, TextUtils.join(",", d.getTags()));
            db.insert(DataContract.GroupsEntry.TABLE_NAME, "null", values);
            // success of transaction
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TagsManager.class.getSimpleName(), "Error adding default group.", e);
        } finally {
            db.endTransaction();
        }

        Log.d(TAG_DB_UPGRADE, "Fetching all expenses not in default group.");
        // add all expenses to default group
        List<Integer> expenseIds = new ArrayList<>();

        String query = String.format("SELECT DISTINCT %s FROM %s " +
                        "WHERE %s NOT IN " +
                        "(SELECT DISTINCT %s FROM %s WHERE %s=?)",
                DataContract.ExpenseEntry._ID, DataContract.ExpenseEntry.TABLE_NAME,
                DataContract.ExpenseEntry._ID, DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE,
                DataContract.ExpenseTagsEntry.TABLE_NAME, DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG);

        Cursor cursor = db.rawQuery(query, new String[]{Group.DEFAULT_TAG});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            expenseIds.add(cursor.getInt(0));
            cursor.moveToNext();
        }
        cursor.close();

        for(int expenseId : expenseIds) {
            try {
                db.beginTransaction();

                // add default tag
                ContentValues values;
                values = new ContentValues();
                values.put(DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE, expenseId);
                values.put(DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG, Group.DEFAULT_TAG);
                db.insert(DataContract.ExpenseTagsEntry.TABLE_NAME, "null", values);
                // success of transaction
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e(TagsManager.class.getSimpleName(), "Error adding tags for expense.", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    private void createGroupsDefault(SQLiteDatabase db) {
        Log.d(TAG_DB_UPGRADE, "Recreating groups table. All existing groups will be lost.");
        db.execSQL(DataContract.SQL_DELETE_GROUPS);
        db.execSQL(DataContract.SQL_CREATE_GROUPS);

        Log.d(TAG_DB_UPGRADE, "Creating default group.");
        // create default group
        try {
            db.beginTransaction();

            Group d = Group.getDefaultGroup();

            ContentValues values;
            values = new ContentValues();
            values.put(DataContract.GroupsEntry.COLUMN_NAME_NAME, d.getName());
            values.put(DataContract.GroupsEntry.COLUMN_NAME_DESCRIPTION, d.getDescription());
            values.put(DataContract.GroupsEntry.COLUMN_NAME_TAGS, TextUtils.join(",", d.getTags()));
            db.insert(DataContract.GroupsEntry.TABLE_NAME, "null", values);
            // success of transaction
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TagsManager.class.getSimpleName(), "Error adding default group.", e);
        } finally {
            db.endTransaction();
        }
    }

}
