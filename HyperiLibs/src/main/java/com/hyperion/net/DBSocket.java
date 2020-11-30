package com.hyperion.net;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.ServerSocket;
import java.util.Iterator;

import javafx.application.Platform;

public class DBSocket extends NetEP {

    public DBSocket() {
        super();
    }

    @Override
    public void init() throws Exception {
        serverSocket = new ServerSocket(Constants.getInt("net.port"));
        clientSocket = serverSocket.accept();
    }

    @Override
    protected void onConnected(Message msg) {
        netLog("Connected to device \"" + msg.sender + "\"");

        sendMessage(Message.Event.CONSTANTS_UPDATED, IOUtils.readDataJSON("constants"));

        JSONArray fieldEdits = new JSONArray();
        JSONObject root = new JSONObject(IOUtils.readDataJSON("field"));
        JSONObject waypointsObject = root.getJSONObject("waypoints");
        JSONObject splinesObject = root.getJSONObject("splines");
        for (Iterator<String> keys = waypointsObject.keys(); keys.hasNext();) {
            String key = keys.next();
            fieldEdits.put(new com.hyperion.net.FieldEdit(new ID(key), com.hyperion.net.FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
        }
        for (Iterator<String> keys = splinesObject.keys(); keys.hasNext();) {
            String key = keys.next();
            fieldEdits.put(new com.hyperion.net.FieldEdit(new ID(key), FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
        }
        sendMessage(Message.Event.FIELD_EDITED, fieldEdits);
    }

    @Override
    protected void onDisconnected(Message msg) {
        netLog("Disconnected from device \"" + msg.sender + "\"");
    }

    @Override
    protected void onConstantsUpdated(Message msg) {
        netLog("Constants updated by device \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();

        Dashboard.constantsSave = msg.json;
        if (Dashboard.leftPane != null) {
            Dashboard.leftPane.setConstantsDisplayText(Constants.root.toString(4));
        }
    }

    @Override
    protected void onMetricsUpdated(Message msg) {
        netLog("Metrics updated by device \"" + msg.sender + "\"");

        Dashboard.readUnimetry(msg.json);
        Platform.runLater(() -> Dashboard.rightPane.setMetricsDisplayText());
    }

    @Override
    protected void onOpModeEnded(Message msg) {
        netLog("OpMode ended in device \"" + msg.sender + "\"");

        Thread deleteRobotThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - start >= 5000) {
                    Iterator<FieldObject> iter = Dashboard.fieldObjects.iterator();
                    while (iter.hasNext()) {
                        FieldObject o = iter.next();
                        if (o.id.equals("robot")) {
                            o.removeDisplayGroup();
                            iter.remove();
                            break;
                        }
                    }
                    Dashboard.isRobotOnField = false;
                    break;
                }
            }
        });
        deleteRobotThread.start();
    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onFieldEdited(Message msg) {

    }

}
