package com.hyperion.dashboard.net;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnectionNotifier;

import javafx.application.Platform;

public class BTServer extends BTEndpoint {

    public BTServer() {
        super();
    }

    @Override
    public void btInit() throws Exception {
        UUID serviceUUID = new UUID(Constants.getString("dashboard.net.serviceUUID"), false);
        String serviceURL = "btspp://localhost:" + serviceUUID.toString() + ";name=HyperionSPP";
        StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open(serviceURL);
        conn = notifier.acceptAndOpen();
    }

    @Override
    protected void onConnected(Message msg) throws Exception {
        printBTLog("Connected to device \"" + msg.sender + "\"");

        sendMessage(Message.Event.CONSTANTS_UPDATED, IOUtils.readDataJSON("constants"));

        JSONArray fieldEdits = new JSONArray();
        JSONObject root = new JSONObject(IOUtils.readDataJSON("field"));
        JSONObject waypointsObject = root.getJSONObject("waypoints");
        JSONObject splinesObject = root.getJSONObject("splines");
        for (Iterator<String> keys = waypointsObject.keys(); keys.hasNext();) {
            String key = keys.next();
            fieldEdits.put(new FieldEdit(new ID(key), FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
        }
        for (Iterator<String> keys = splinesObject.keys(); keys.hasNext();) {
            String key = keys.next();
            fieldEdits.put(new FieldEdit(new ID(key), FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
        }
        sendMessage(Message.Event.FIELD_EDITED, fieldEdits);
    }

    @Override
    protected void onDisconnected(Message msg) throws Exception {
        printBTLog("Disconnected from device \"" + msg.sender + "\"");
    }

    @Override
    protected void onConstantsUpdated(Message msg) throws Exception {
        printBTLog("Constants updated by device \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();

        Dashboard.constantsSave = msg.json;
        if (Dashboard.leftPane != null) {
            Dashboard.leftPane.setConstantsDisplayText(Constants.root.toString(4));
        }
    }

    @Override
    protected void onMetricsUpdated(Message msg) throws Exception {
        printBTLog("Metrics updated by device \"" + msg.sender + "\"");

        Dashboard.readUnimetry(msg.json);
//        Platform.runLater(() -> Dashboard.rightPane.setMetricsDisplayText());
    }

    @Override
    protected void onOpModeEnded(Message msg) throws Exception {
        printBTLog("OpMode ended in device \"" + msg.sender + "\"");

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

    //////////////// MISCELLANEOUS ////////////////

    public static JSONArray readDashboardAsFieldEdits(String json) {
        JSONArray arr = new JSONArray();
        try {
            JSONObject root = new JSONObject(json);
            JSONObject waypointsObject = root.getJSONObject("waypoints");
            JSONObject splinesObject = root.getJSONObject("splines");

            for (Iterator<String> keys = waypointsObject.keys(); keys.hasNext();) {
                String key = keys.next();
                arr.put(new FieldEdit(new ID(key), FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
            }
            for (Iterator<String> keys = splinesObject.keys(); keys.hasNext();) {
                String key = keys.next();
                arr.put(new FieldEdit(new ID(key), FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

}
