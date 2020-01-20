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

    public Options(File file) {
        try {
            this.file = file;
            read(new JSONObject(Utils.readFile(file)));
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
        JSONObject obj = new JSONObject();
        try {
            obj.put("debug", debug);
            obj.put("socketLog", socketLog);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

}
