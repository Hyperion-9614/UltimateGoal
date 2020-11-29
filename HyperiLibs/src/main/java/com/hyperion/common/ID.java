package com.hyperion.common;

import java.util.*;

public class ID {

    public ArrayList<String> segments;

    public ID(Object... segments) {
        this(TextUtils.join(".", segments));
    }

    public ID(String idStr) {
        this.segments = new ArrayList<>(Arrays.asList(idStr.split("\\.")));
    }

    public String get(int i) {
        if (i < 0) i += segments.size();
        return i < segments.size() ? segments.get(i) : "";
    }

    public void set(int i, Object val) {
        if (i < 0) i += segments.size();
        segments.set(i, val.toString());
    }

    public void add(Object segment) {
        segments.add(String.valueOf(segment));
    }

    public void remove(int i) {
        if (i < 0) i += segments.size();
        segments.remove(i);
    }

    public void remove(Object segment) {
        segments.remove(String.valueOf(segment));
    }

    public boolean contains(Object segment) {
        return segments.contains(String.valueOf(segment));
    }

    public boolean contains(ID otherID) {
        return this.toString().contains(otherID.toString());
    }

    public String sub(int start, int end) {
        if (start <= segments.size() - 1) {
            StringBuilder idStr = new StringBuilder();
            for (int i = start; i < end; i++) idStr.append(segments.get(i)).append(".");
            return idStr.substring(0, idStr.length() - 1);
        }
        return "";
    }

    public String sub(int start) {
        return sub(start, segments.size());
    }

    public boolean equals(ID id) {
        return this.toString().equals(id.toString());
    }

    public boolean equals(String idStr) {
        return this.toString().equals(idStr);
    }

    public int size() {
        return segments.size();
    }

    @Override
    public String toString() {
        return sub(0);
    }
}
