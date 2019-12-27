package com.hyperion.common;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.HashMap;

public class Options {

    public boolean debug;
    public boolean socketLog;

    public Options(File file) {
        try {
            JSONTokener tokener = new JSONTokener(Utils.readFile(file));
            JSONObject root = new JSONObject(tokener);
            JSONObject options = root.getJSONObject("options");

            debug = options.getBoolean("debug");
            socketLog = options.getBoolean("socketLog");
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
