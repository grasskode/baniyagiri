package com.grasskode.baniyagiri.adapters;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.elements.AggregatedExpense;
import com.grasskode.baniyagiri.elements.Aggregation;
import com.grasskode.baniyagiri.elements.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by karan on 20/5/16.
 */
public class AggregatedExpenseAdapter extends BaseAdapter {

    private final Context context;
    private List<Expense> allExpenses;
    private AggregatedExpense[] values;
    private Aggregation aggregation;

    public AggregatedExpenseAdapter(Context context, List<Expense> expenses) {
        super();
        this.context = context;
        this.allExpenses = expenses;
        this.values = new AggregatedExpense[]{};
        this.aggregation = null;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @Override
    public Object getItem(int position) {
        return values[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // get aggregated expense and values
        AggregatedExpense ae = values[position];
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(ae.getDatetime());
        String date = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        String monthYear = sdf.format(cal.getTime());
        sdf = new SimpleDateFormat("MMM", Locale.getDefault());
        String month = sdf.format(cal.getTime());
        String year = String.valueOf(cal.get(Calendar.YEAR));
        sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String weekStart = sdf.format(cal.getTime());
        cal.add(Calendar.DATE, 7);
        String weekEnd = sdf.format(cal.getTime());


        View view = convertView;
        switch (aggregation) {
            case DAILY:
                view = inflater.inflate(R.layout.view_expense_daily_item, parent, false);
                ((TextView) view.findViewById(R.id.expense_date)).setText(date);
                ((TextView) view.findViewById(R.id.expense_month_year)).setText(monthYear);
                break;
            case WEEKLY:
                view = inflater.inflate(R.layout.view_expense_weekly_item, parent, false);
                ((TextView) view.findViewById(R.id.expense_week_start)).setText(weekStart);
                ((TextView) view.findViewById(R.id.expense_week_end)).setText(weekEnd);
                break;
            case MONTHLY:
                view = inflater.inflate(R.layout.view_expense_monthly_item, parent, false);
                ((TextView) view.findViewById(R.id.expense_month)).setText(month);
                ((TextView) view.findViewById(R.id.expense_year)).setText(year);
                break;
            case YEARLY:
                view = inflater.inflate(R.layout.view_expense_yearly_item, parent, false);
                ((TextView) view.findViewById(R.id.expense_year)).setText(year);
                break;
        }

        final LinearLayout aggContainer = (LinearLayout) view.findViewById(R.id.agg_container);
        for (String curr : ae.getAggregates().keySet()) {
            LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lparams.setMargins(0, 0, 3, 3);
            lparams.gravity = Gravity.CENTER;
            View aeV = inflater.inflate(R.layout.view_agg_curr_amt, null);
            aeV.setLayoutParams(lparams);
            ((TextView) aeV.findViewById(R.id.expense_currency)).setText(curr);
            ((TextView) aeV.findViewById(R.id.expense_amount)).setText(String.format("%.2f",
                    ae.getAggregates().get(curr)));
            aggContainer.addView(aeV);
        }

        return view;
    }

    public void aggregateFor(Aggregation aggregation) {
        this.aggregation = aggregation;

        Map<Long, AggregatedExpense> aggExpenses = new HashMap<>();
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        for(Expense ex : allExpenses) {
            cal.setTimeInMillis(ex.getDatetime());
            switch (aggregation) {
                case YEARLY:
                    cal.set(Calendar.MONTH, Calendar.JANUARY);
                case MONTHLY:
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                case DAILY:
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    break;
                case WEEKLY:
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    break;
            }
            long timestamp = (cal.getTimeInMillis()/1000)*1000;

            if(!aggExpenses.containsKey(timestamp)) {
                aggExpenses.put(timestamp, new AggregatedExpense(timestamp));
            }

            aggExpenses.get(timestamp).addExpense(ex.getCurrency(), ex.getAmount());
        }

        List<Long> timestamps = new ArrayList<>(aggExpenses.keySet());
        Collections.sort(timestamps);
        Collections.reverse(timestamps);

        List<AggregatedExpense> sorted = new ArrayList<>(timestamps.size());
        for(Long ts : timestamps) {
            sorted.add(aggExpenses.get(ts));
        }

        values = new AggregatedExpense[sorted.size()];
        sorted.toArray(values);
    }
}
