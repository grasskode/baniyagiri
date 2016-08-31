package com.grasskode.baniyagiri.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by karan on 19/5/16.
 */
public class TagsListAdapter extends ArrayAdapter<String> implements Filterable {

    private List<String> completeList;
    private List<String> values;
    private ArrayFilter filter;

    public TagsListAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
        completeList = objects;
        values = new ArrayList<>(completeList);
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public String getItem(int position) {
        return values.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new ArrayFilter();
        }
        return filter;
    }


    private class ArrayFilter extends Filter {
        private Object lock;

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            List<String> list = new ArrayList<>(completeList);
            if (prefix != null && prefix.length() > 0) {
                list.clear();
                final String prefixString = prefix.toString().toLowerCase();
                for(String v : completeList) {
                    if(v.toLowerCase().startsWith(prefixString)) {
                        list.add(v);
                    }
                }
            }
            results.values = list;
            results.count = list.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if(results.values != null) {
                values = (List<String>) results.values;
            } else {
                values = new ArrayList<>();
            }
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
