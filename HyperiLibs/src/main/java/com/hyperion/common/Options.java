package com.hyperion.common;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.HashMap;

public class Options {

    public File file;
    public JSONObject root;

    public boolean debug;
    public boolean socketLog;

    public Options() {

    }

    public Options(File file) {
        try {
            this.file = file;
            JSONTokener tokener = new JSONTokener(Utils.readFile(file));
            this.root = new JSONObject(tokener);

            debug = root.getBoolean("debug");
            socketLog = root.getBoolean("socketLog");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read(JSONObject root) {
        try {
            this.root = root;
            debug = root.getBoolean("debug");
            socketLog = root.getBoolean("socketLog");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSONObject() {
        HashMap<String, String> map = new HashMap<>();
        map.put("debug", String.valueOf(debug));
        map.put("socketLog", String.valueOf(socketLog));
        return new JSONObject(map);
    }

}
