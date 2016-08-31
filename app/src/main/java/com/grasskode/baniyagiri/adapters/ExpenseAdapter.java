package com.grasskode.baniyagiri.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.elements.Expense;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by karan on 20/5/16.
 */
public class ExpenseAdapter extends BaseAdapter {

    private final Context context;
    private List<Expense> values;

    public ExpenseAdapter(Context context, List<Expense> expenses) {
        super();
        this.context = context;
        this.values = expenses;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return values.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // get expense and values
        Expense expense = values.get(position);
        String name = expense.getName();
        String currency = expense.getCurrency();
        String amount = String.format("%.2f", expense.getAmount());
        String date = "?";
        String monthYear = "";
        if(expense.getDatetime() > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(expense.getDatetime());
            date = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            monthYear = sdf.format(cal.getTime());
        }

        // inflate or reuse view
        View view = convertView;
        if(convertView == null)
            view = inflater.inflate(R.layout.view_expense_list_item, parent, false);

        TextView dateView = (TextView) view.findViewById(R.id.expense_date);
        dateView.setText(date);
        TextView monthYearView = (TextView) view.findViewById(R.id.expense_month_year);
        monthYearView.setText(monthYear);
        TextView currView = (TextView) view.findViewById(R.id.expense_currency);
        currView.setText(currency);
        TextView amountView = (TextView) view.findViewById(R.id.expense_amount);
        amountView.setText(amount);
        TextView nameView = (TextView) view.findViewById(R.id.expense_name);
        nameView.setText(name);

        return view;
    }

}
