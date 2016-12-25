package com.grasskode.baniyagiri.elements;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by karan on 18/5/16.
 */
public class Expense {

    int id;
    String name;
    long datetime;
    String currency;
    float amount;
    List<String> tags;

    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public Expense() {
        this.tags = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String[] toCSVRow() {
        String[] values = new String[5];
        values[0] = sdfDate.format(new Date(getDatetime()));
        values[1] = getName();
        values[2] = getCurrency();
        values[3] = String.valueOf(getAmount());
        values[4] = TextUtils.join(",", getTags());
        return values;
    }

    public static Expense fromCSVRow(String[] values) {
        if(values.length != 5) {
            return null;
        }

        Expense e = new Expense();
        try {
            e.setDatetime(sdfDate.parse(values[0]).getTime());
            e.setName(values[1]);
            e.setCurrency(values[2]);
            e.setAmount(Float.valueOf(values[3]));
            e.setTags(new ArrayList<>(Arrays.asList(values[4].split(","))));
        } catch (ParseException ex) {
            return null;
        }
        return e;
    }
}
