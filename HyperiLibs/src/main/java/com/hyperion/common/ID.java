package com.hyperion.common;

import java.util.*;

public class ID {

    public ArrayList<String> args;

    public ID(Object... args) {
        this(TextUtils.join(".", args));
    }

    public ID(String idStr) {
        this.args = new ArrayList<>(Arrays.asList(idStr.split("\\.")));
    }

    public String get(int i) {
        if (i < 0) i += args.size();
        return i < args.size() ? args.get(i) : "";
    }

    public void set(int i, Object val) {
        if (i < 0) i += args.size();
        args.set(i, val.toString());
    }

    public void add(Object val) {
        args.add(val.toString());
    }

    public void remove(int i) {
        if (i < 0) i += args.size();
        args.remove(i);
    }

    public void remove(Object val) {
        args.remove(val);
    }

    public boolean contains(String val) {
        return args.contains(val);
    }

    public boolean contains(ID otherID) {
        return this.toString().contains(otherID.toString());
    }

    public String sub(int start, int end) {
        if (start <= args.size() - 1) {
            StringBuilder idStr = new StringBuilder();
            for (int i = start; i < end; i++) idStr.append(args.get(i)).append(".");
            return idStr.substring(0, idStr.length() - 1);
        }
        return "";
    }

    public String sub(int start) {
        return sub(start, args.size());
    }

    public boolean equals(ID id) {
        return this.toString().equals(id.toString());
    }

    public boolean equals(String idStr) {
        return this.toString().equals(idStr);
    }

    public int size() {
        return args.size();
    }

    @Override
    public String toString() {
        return sub(0);
    }
}
