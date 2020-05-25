package com.hyperion.dashboard.net;

import com.hyperion.common.Constants;
import com.hyperion.common.IOUtils;
import com.hyperion.dashboard.UIMain;
import com.hyperion.dashboard.uiobject.FieldObject;

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
        for (Iterator keys = waypointsObject.keys(); keys.hasNext();) {
            String key = keys.next().toString();
            fieldEdits.put(new FieldEdit(key, FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
        }
        for (Iterator keys = splinesObject.keys(); keys.hasNext();) {
            String key = keys.next().toString();
            fieldEdits.put(new FieldEdit(key, FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
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

        UIMain.constantsSave = msg.json;
        if (UIMain.rightPane != null) {
            UIMain.rightPane.setConstantsDisplayText(Constants.root.toString(4));
        }
    }

    @Override
    protected void onMetricsUpdated(Message msg) throws Exception {
        printBTLog("Metrics updated by device \"" + msg.sender + "\"");

        UIMain.readUnimetry(msg.json);
        Platform.runLater(() -> UIMain.rightPane.setMetricsDisplayText());
    }

    @Override
    protected void onOpModeEnded(Message msg) throws Exception {
        printBTLog("OpMode ended in device \"" + msg.sender + "\"");

        Thread deleteRobotThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - start >= 5000) {
                    Iterator<FieldObject> iter = UIMain.fieldObjects.iterator();
                    while (iter.hasNext()) {
                        FieldObject o = iter.next();
                        if (o.id.equals("robot")) {
                            o.removeDisplayGroup();
                            iter.remove();
                            break;
                        }
                    }
                    UIMain.isRobotOnField = false;
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

            for (Iterator keys = waypointsObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                arr.put(new FieldEdit(key, FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
            }
            for (Iterator keys = splinesObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                arr.put(new FieldEdit(key, FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

}
