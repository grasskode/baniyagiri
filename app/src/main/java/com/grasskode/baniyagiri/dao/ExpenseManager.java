package com.grasskode.baniyagiri.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.grasskode.baniyagiri.elements.Expense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by karan on 18/5/16.
 */
public class ExpenseManager {

    Context context;
    DatabaseHelper dbHelper;
    TagsManager tagsManager;

    static final String[] expense_all_columns = {
            DataContract.ExpenseEntry._ID,
            DataContract.ExpenseEntry.COLUMN_NAME_NAME,
            DataContract.ExpenseEntry.COLUMN_NAME_DATETIME,
            DataContract.ExpenseEntry.COLUMN_NAME_CURRENCY,
            DataContract.ExpenseEntry.COLUMN_NAME_AMOUNT
    };

    public ExpenseManager(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
        tagsManager = new TagsManager(context);
    }

    public Expense editExpense(Expense expense) {
        if(expense.getId() > 0) {
            Log.d(ExpenseManager.class.getSimpleName(), "Deleting old expense with ID "+expense.getId());
            deleteExpense(expense.getId());
        }

        Log.d(ExpenseManager.class.getSimpleName(), "Creating new expense.");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DataContract.ExpenseEntry.COLUMN_NAME_NAME, expense.getName());
        values.put(DataContract.ExpenseEntry.COLUMN_NAME_DATETIME, expense.getDatetime());
        values.put(DataContract.ExpenseEntry.COLUMN_NAME_CURRENCY, expense.getCurrency());
        values.put(DataContract.ExpenseEntry.COLUMN_NAME_AMOUNT, expense.getAmount());


        long expenseId = db.insert(DataContract.ExpenseEntry.TABLE_NAME, "null", values);
        db.close();
        Log.d(ExpenseManager.class.getSimpleName(), String.format("Created expense with ID %d.", expenseId));

        // set expense ID
        expense.setId((int) expenseId);

        // add expense tags
        tagsManager.addExpenseTags(expense.getId(), expense.getTags());

        // return expense with ID
        return expense;
    }

    public Expense getExpense(int expenseId) {
        Log.d(ExpenseManager.class.getSimpleName(), String.format("Getting expense with ID %d.", expenseId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.ExpenseEntry.TABLE_NAME, expense_all_columns,
                DataContract.ExpenseEntry._ID + " = " + expenseId, null, null, null,
                null);
        Expense expense = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            expense = cursorToExpense(cursor);
        }
        cursor.close();
        db.close();

        return expense;
    }

    public boolean deleteExpense(int expenseId) {
        Log.d(ExpenseManager.class.getSimpleName(), String.format("Deleting expense with ID %d.", expenseId));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int affected = db.delete(DataContract.ExpenseEntry.TABLE_NAME,
                DataContract.ExpenseEntry._ID + " = " + expenseId, null);
        db.close();

        // get expense tags
        tagsManager.deleteExpenseTags(expenseId);

        return affected > 0;
    }

    public List<Integer> getExpenseIds(long start, long end, List<String> tags, List<String> negativeTags) {
        Log.d(ExpenseManager.class.getSimpleName(), String.format("Getting expenses for -> start %d | end %d | tags %s",
                start, end, (tags == null)?"null":TextUtils.join(",", tags)));

        String select = "SELECT DISTINCT e."+ DataContract.ExpenseEntry._ID+
                " FROM "+ DataContract.ExpenseEntry.TABLE_NAME+" e";
        String where = "";
        List<String> params = new ArrayList<>();

        if (start > 0) {
            if(!where.isEmpty())
                where += " AND ";
            where += "e."+ DataContract.ExpenseEntry.COLUMN_NAME_DATETIME+" >= ?";
            params.add(String.valueOf(start));
        }
        if (end > 0) {
            if(!where.isEmpty())
                where += " AND ";
            where += " e."+ DataContract.ExpenseEntry.COLUMN_NAME_DATETIME+" < ?";
            params.add(String.valueOf(end));
        }

        List<Integer> expenseIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (tags != null && !tags.isEmpty()) {
            String iterWhere = where;
            if(!iterWhere.isEmpty())
                iterWhere += " AND ";
            iterWhere += "et."+ DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE
                    + " = e."+ DataContract.ExpenseEntry._ID
                    + " AND et."+ DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG+" = ?";
            List<Integer> andList = null;
            for (String tag : tags) {
                String query = select + ", " + DataContract.ExpenseTagsEntry.TABLE_NAME+" et"
                        +" WHERE " + iterWhere
                        + " ORDER BY e."+ DataContract.ExpenseEntry.COLUMN_NAME_DATETIME+" DESC";

                List<String> iterParams = new ArrayList<>(params);
                iterParams.add(tag);
                String[] selectionArgs = null;
                selectionArgs = new String[iterParams.size()];
                iterParams.toArray(selectionArgs);

                List<Integer> ids = new ArrayList<>();
                Cursor cursor = db.rawQuery(query, selectionArgs);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ids.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
                cursor.close();

                if (andList == null) {
                    // for first iteration
                    andList = ids;
                } else {
                    // perform AND
                    List<Integer> tempList = new ArrayList<>();
                    for (int id : ids) {
                        if(andList.contains(id)) {
                            tempList.add(id);
                        }
                    }
                    andList = tempList;
                }

                if(andList.isEmpty()) {
                    // break and set and to empty if and becomes empty anytime
                    break;
                }
            }
            if(andList == null)
                andList = new ArrayList<>();
            expenseIds = andList;
        } else {
            String query = select;

            String[] selectionArgs = null;
            if (!where.isEmpty()) {
                query += " WHERE "+where;
                selectionArgs = new String[params.size()];
                params.toArray(selectionArgs);
            }

            query += " ORDER BY e."+ DataContract.ExpenseEntry.COLUMN_NAME_DATETIME+" DESC";

            Cursor cursor = db.rawQuery(query, selectionArgs);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                expenseIds.add(cursor.getInt(0));
                cursor.moveToNext();
            }
            cursor.close();
        }

        if (negativeTags != null && !negativeTags.isEmpty()) {
            String iterWhere = where;
            if(!iterWhere.isEmpty())
                iterWhere += " AND ";
            iterWhere += "et."+ DataContract.ExpenseTagsEntry.COLUMN_NAME_EXPENSE
                    + " = e."+ DataContract.ExpenseEntry._ID
                    + " AND et."+ DataContract.ExpenseTagsEntry.COLUMN_NAME_TAG+" = ?";
            for (String tag : negativeTags) {
                String query = select + ", " + DataContract.ExpenseTagsEntry.TABLE_NAME+" et"
                        +" WHERE " + iterWhere
                        + " ORDER BY e."+ DataContract.ExpenseEntry.COLUMN_NAME_DATETIME+" DESC";

                List<String> iterParams = new ArrayList<>(params);
                iterParams.add(tag);
                String[] selectionArgs = null;
                selectionArgs = new String[iterParams.size()];
                iterParams.toArray(selectionArgs);

                List<Integer> ids = new ArrayList<>();
                Cursor cursor = db.rawQuery(query, selectionArgs);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ids.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
                cursor.close();

                // perform NOT
                List<Integer> tempList = new ArrayList<>();
                for (int id : expenseIds) {
                    if(!ids.contains(id)) {
                        tempList.add(id);
                    }
                }
                expenseIds = tempList;
            }
        }

        db.close();

        return expenseIds;
    }

    public List<Expense> getExpenses(long start, long end, List<String> tags, List<String> negativeTags) {
        List<Integer> expenseIds = getExpenseIds(start, end, tags, negativeTags);
        String[] idsArr = new String[expenseIds.size()];
        for(int i=0; i<expenseIds.size(); i++) {
            idsArr[i] = String.valueOf(expenseIds.get(i));
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DataContract.ExpenseEntry.TABLE_NAME, expense_all_columns,
                DataContract.ExpenseEntry._ID + " IN (" +
                        TextUtils.join(",", Collections.nCopies(idsArr.length, "?"))+")",
                idsArr, null, null, null);

        Map<Integer, Expense> expenseMap = new HashMap<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Expense expense = cursorToExpense(cursor);
            expenseMap.put(expense.getId(), expense);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();

        List<Expense> expenses = new ArrayList<>(expenseIds.size());
        for(int eId : expenseIds) {
            expenses.add(expenseMap.get(eId));
        }
        return expenses;
    }

    private Expense cursorToExpense(Cursor cursor) {
        Expense expense = new Expense();
        expense.setId(cursor.getInt(0));
        expense.setName(cursor.getString(1));
        expense.setDatetime(cursor.getLong(2));
        expense.setCurrency(cursor.getString(3));
        expense.setAmount(cursor.getFloat(4));
        return expense;
    }

}
