package com.grasskode.baniyagiri.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.grasskode.baniyagiri.R;
import com.grasskode.baniyagiri.elements.Group;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by karan on 27/7/16.
 */
public class GroupsAdapter extends BaseAdapter {

    private final Context context;
    private List<Group> values;

    public GroupsAdapter(Context context, List<Group> groups) {
        super();
        this.context = context;
        this.values = groups;
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

        // get group and values
        Group group = values.get(position);
        String name = group.getName();
        String desc = group.getDescription();
        String startDateTime = "?";
        String endDatetime = "?";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        if(group.getStartDatetime() > 0) {
            cal.setTimeInMillis(group.getStartDatetime());
            startDateTime = sdf.format(cal.getTime());
        }
        if(group.getEndDatetime() > 0) {
            cal.setTimeInMillis(group.getEndDatetime());
            endDatetime = sdf.format(cal.getTime());
        }

        // inflate or reuse view
        View view = convertView;
        if(convertView == null)
            view = inflater.inflate(R.layout.view_group_list_item, parent, false);

        TextView nameView = (TextView) view.findViewById(R.id.group_name);
        nameView.setText(name);
        TextView descriptionView = (TextView) view.findViewById(R.id.group_description);
        descriptionView.setText(desc);
        TextView startDatetimeView = (TextView) view.findViewById(R.id.group_start_datetime);
        startDatetimeView.setText(startDateTime);
        TextView endDatetimeView = (TextView) view.findViewById(R.id.group_end_datetime);
        endDatetimeView.setText(endDatetime);
        if(group.getStartDatetime() == 0 && group.getEndDatetime() == 0) {
            view.findViewById(R.id.dates_layout).setVisibility(View.INVISIBLE);
        } else {
            view.findViewById(R.id.dates_layout).setVisibility(View.VISIBLE);
        }

        return view;
    }

}
