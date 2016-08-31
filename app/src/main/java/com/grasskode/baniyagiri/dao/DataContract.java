package com.grasskode.baniyagiri.dao;

import android.provider.BaseColumns;

/**
 * Created by karan on 18/5/16.
 */
public class DataContract {

    public DataContract() {}

    // expenses table
    public static class ExpenseEntry implements BaseColumns {
        public static final String TABLE_NAME = "expenses";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DATETIME = "datetime";
        public static final String COLUMN_NAME_AMOUNT = "amount";
        public static final String COLUMN_NAME_CURRENCY = "currency_id";
    }
    protected static final String SQL_DELETE_EXPENSES =
            "DROP TABLE IF EXISTS " + ExpenseEntry.TABLE_NAME;
    protected static final String SQL_CREATE_EXPENSES =
            "CREATE TABLE " + ExpenseEntry.TABLE_NAME + " (" +
                    ExpenseEntry._ID + " INTEGER PRIMARY KEY, " +
                    ExpenseEntry.COLUMN_NAME_NAME + " text, " +
                    ExpenseEntry.COLUMN_NAME_DATETIME + " integer, "+
                    ExpenseEntry.COLUMN_NAME_AMOUNT + " real, "+
                    ExpenseEntry.COLUMN_NAME_CURRENCY + " text "+
                    " )";

    // tags table
    public static class ExpenseTagsEntry implements BaseColumns {
        public static final String TABLE_NAME = "expense_tags";
        public static final String COLUMN_NAME_EXPENSE = "expense_id";
        public static final String COLUMN_NAME_TAG = "tag";
    }
    protected static final String SQL_DELETE_EXPENSE_TAGS =
            "DROP TABLE IF EXISTS " + ExpenseTagsEntry.TABLE_NAME;
    protected static final String SQL_CREATE_EXPENSE_TAGS =
            "CREATE TABLE " + ExpenseTagsEntry.TABLE_NAME + " (" +
                    ExpenseTagsEntry._ID + " INTEGER PRIMARY KEY, " +
                    ExpenseTagsEntry.COLUMN_NAME_EXPENSE + " integer, " +
                    ExpenseTagsEntry.COLUMN_NAME_TAG + " text "+
                    " )";

    // groups table
    public static class GroupsEntry implements BaseColumns {
        public static final String TABLE_NAME = "groups";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_TAGS = "tags";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }
    protected static final String SQL_DELETE_GROUPS =
            "DROP TABLE IF EXISTS " + GroupsEntry.TABLE_NAME;
    protected static final String SQL_CREATE_GROUPS =
            "CREATE TABLE " + GroupsEntry.TABLE_NAME + " (" +
                    GroupsEntry._ID + " INTEGER PRIMARY KEY, " +
                    GroupsEntry.COLUMN_NAME_NAME + " text not null unique, "+
                    GroupsEntry.COLUMN_NAME_DESCRIPTION + " text, "+
                    GroupsEntry.COLUMN_NAME_TAGS + " text, "+
                    GroupsEntry.COLUMN_NAME_TIMESTAMP + " integer "+
                    " )";

}
