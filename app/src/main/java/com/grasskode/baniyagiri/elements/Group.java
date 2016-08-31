package com.grasskode.baniyagiri.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Group element.
 *
 * Created by karan on 28/7/16.
 */
public class Group {

    public static final String DEFAULT_NAME = "All Expenses";
    public static final String DEFAULT_DESCRIPTION = "This is the default group. All expenses belong to this group.";
    public static final String DEFAULT_TAG = "_default";

    int id;
    String name;
    String description;
    List<String> tags;
    long startDatetime;
    long endDatetime;
    long timestamp;

    public Group() {
        this.tags = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStartDatetime() {
        return startDatetime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setStartDatetime(long startDatetime) {
        this.startDatetime = startDatetime;
    }

    public long getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(long endDatetime) {
        this.endDatetime = endDatetime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static Group getDefaultGroup() {
        Group def = new Group();
        def.setName(DEFAULT_NAME);
        def.setDescription(DEFAULT_DESCRIPTION);
        def.addTag(DEFAULT_TAG);
        return def;
    }
}
