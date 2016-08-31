package com.grasskode.baniyagiri.elements;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by karan on 20/5/16.
 */
public class AggregatedExpense {

    long datetime;
    Map<String, Float> aggregates;

    public AggregatedExpense(long datetime) {
        this.datetime = datetime;
        this.aggregates = new HashMap<>();
    }

    public long getDatetime() {
        return datetime;
    }

    public Map<String, Float> getAggregates() {
        return aggregates;
    }

    public void addExpense(String curr, float amount) {
        float temp = amount;
        if(aggregates.containsKey(curr)) {
            temp += aggregates.get(curr);
        }
        aggregates.put(curr, temp);
    }
}
