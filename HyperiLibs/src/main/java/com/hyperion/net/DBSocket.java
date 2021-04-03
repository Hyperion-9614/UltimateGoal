package com.hyperion.net;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;

import javafx.application.Platform;

public class DBSocket extends NetEP {

    public DBSocket() {
        super();
    }

    @Override
    public void init() throws Exception {
        sender = Message.Sender.DASHBOARD;
        serverSocket = new ServerSocket(Constants.getInt("net.port"));
        String iNetAddr = InetAddress.getLocalHost().toString();
        Constants.setAtID("net.dbIP", iNetAddr.substring(iNetAddr.indexOf('/') + 1).trim(), true);
        clientSocket = serverSocket.accept();
    }

    @Override
    protected void onConnected(Message msg) {
        String remote = clientSocket.getRemoteSocketAddress().toString();
        netLog(LogLevel.INFO, "Connected to \"" + msg.sender + "\" [IP: " + remote.substring(remote.indexOf('/') + 1).trim() + "]");

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
        String remote = clientSocket.getRemoteSocketAddress().toString();
        netLog(LogLevel.INFO, "Disconnected from \"" + msg.sender + "\" [IP: " + remote.substring(remote.indexOf('/') + 1).trim() + "]");
    }

    @Override
    protected void onConstantsUpdated(Message msg) {
        netLog(LogLevel.INFO, "Constants updated by \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();

        Dashboard.leftPane.constantsSave = msg.json;
        if (Dashboard.leftPane != null) {
            Dashboard.leftPane.setConstantsDisplayText(Constants.root.toString(4));
        }
    }

    @Override
    protected void onMetricsUpdated(Message msg) {
        netLog(LogLevel.INFO, "Metrics updated by \"" + msg.sender + "\"");

        JSONObject metricsObj = new JSONObject(msg.json);

        // Update telemetry
        HashMap<String, String> telemetry = new HashMap<>();
        JSONObject dataObj = metricsObj.getJSONObject("telemetry");
        for (String key : dataObj.keySet()) {
            telemetry.put(key, dataObj.getString(key));
        }
        Platform.runLater(() -> Dashboard.rightPane.setTelemetryDisplayText(telemetry));

        // Update robot position
        Dashboard.editField(new FieldEdit(new ID("robot"), Dashboard.fieldPane.isRobotOnField ? FieldEdit.Type.EDIT_BODY : FieldEdit.Type.CREATE,
                new JSONArray(new RigidBody(telemetry.get("Current")).toArray()).toString()));
        Dashboard.fieldPane.isRobotOnField = true;

        // Update velocity motors graph
        JSONObject silentObj = metricsObj.getJSONObject("silent");
        JSONObject velMotors = silentObj.getJSONObject("velMotors");
        if (Dashboard.visualPane.velMotors.size() == 0) {
            Dashboard.visualPane.velMotors.addAll(velMotors.keySet());
            Dashboard.visualPane.velMotorSelector.valueProperty()
                    .setValue(Dashboard.visualPane.velMotors.get(0));
        }
        JSONObject motorData = velMotors.getJSONObject(Dashboard.visualPane.velMotorSelector.valueProperty().getValue());
        Dashboard.visualPane.updateVelMotorGraph(motorData.getDouble("currRPM"), motorData.getDouble("targetRPM"));
    }

    @Override
    protected void onOpModeEnded(Message msg) {
        netLog(LogLevel.INFO, "OpMode ended in \"" + msg.sender + "\"");

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
                    Dashboard.fieldPane.isRobotOnField = false;
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

    @Override
    protected void onSignal(Message msg) {

    }

}
