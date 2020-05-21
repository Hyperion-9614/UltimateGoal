package com.hyperion.dashboard;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.hyperion.common.Constants;
import com.hyperion.common.IOUtils;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.net.FieldEdit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Runs dashboard server socket
 */
public class ServerMain {

    public static SocketIOServer server;
    public static ArrayList<SocketIOClient> dashboardClients = new ArrayList<>();
    public static SocketIOClient rcClient;

    public static void main(String[] args) {
        Constants.init(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));

        try {
            Configuration configuration = new Configuration();
            Constants.setAtID("dashboard.net.hostIP", InetAddress.getLocalHost().getHostAddress());
            Constants.write();
            configuration.setHostname(Constants.getString("dashboard.net.hostIP"));
            configuration.setPort(Constants.getInt("dashboard.net.port"));
            server = new SocketIOServer(configuration);

            server.addConnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                                 .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                                 .replace("/", "");
                String type;
                if (rcClient == null && address.equals("192.168.49.1")) {
                    rcClient = client;
                    type = "RC";
                } else {
                    dashboardClients.add(client);
                    type = "UI";
                }
                TextUtils.printSocketLog(type, "SERVER", address + " connected");

                client.sendEvent("constantsUpdated", IOUtils.readDataJSON("constants"));
                TextUtils.printSocketLog("SERVER", type, "constantsUpdated");

                client.sendEvent("fieldEdited", readDashboardAsFieldEdits(IOUtils.readDataJSON("field")).toString());
                TextUtils.printSocketLog("SERVER", type, "fieldEdited");
            });

            server.addDisconnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                if (rcClient != null && address.equals("192.168.49.1")) {
                    rcClient = null;
                    TextUtils.printSocketLog("RC", "SERVER", "disconnected");
                } else {
                    TextUtils.printSocketLog("UI", "SERVER", "disconnected");
                    dashboardClients.remove(client);
                }
            });

            server.addEventListener("fieldEdited", String.class, (client, data, ackRequest) -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                String type = (rcClient != null && address.equals("192.168.49.1")) ? "RC" : "UI";
                TextUtils.printSocketLog(type, "SERVER", "fieldEdited");

                for (SocketIOClient dashboardClient : dashboardClients) {
                    if (!dashboardClient.getRemoteAddress().equals(client.getRemoteAddress())) {
                        dashboardClient.sendEvent("fieldEdited", data);
                        TextUtils.printSocketLog("SERVER", "UI", "fieldEdited");
                    }
                }
                if (rcClient != null) {
                    rcClient.sendEvent("fieldEdited", data);
                    TextUtils.printSocketLog("SERVER", "RC", "fieldEdited");
                }

                writeEditsToFieldJSON(data);
            });

            server.addEventListener("opModeEnded", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("opModeEnded", data);
                    TextUtils.printSocketLog("SERVER", "UI", "opModeEnded");
                }
            });

            server.addEventListener("unimetryUpdated", String.class, (client, data, ackRequest) -> {
                TextUtils.printSocketLog("RC", "SERVER", "unimetryUpdated");

                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("unimetryUpdated", data);
                    TextUtils.printSocketLog("SERVER", "UI", "unimetryUpdated");
                }
            });

            server.addEventListener("constantsUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    if (!dashboardClient.getRemoteAddress().equals(client.getRemoteAddress())) {
                        dashboardClient.sendEvent("constantsUpdated", data);
                        TextUtils.printSocketLog("SERVER", "UI", "constantsUpdated");
                    }
                }
                if (rcClient != null) {
                    rcClient.sendEvent("constantsUpdated", data);
                    TextUtils.printSocketLog("SERVER", "RC", "constantsUpdated");
                }

                Constants.init(new JSONObject(data));
                Constants.write();
            });

            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read in waypoints & splines from json as JSONArray of field edits
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

    public static void writeEditsToFieldJSON(String json) {
        try {
            JSONObject field = new JSONObject(IOUtils.readDataJSON("field"));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit edit = new FieldEdit(arr.getJSONObject(i));
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id, (edit.id.contains("waypoint") ? new JSONArray(edit.body) : new JSONObject(edit.body)));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = target.getJSONArray(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = target.getJSONObject(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            target.remove(edit.id);
                            break;
                    }
                    field.put(o, target);
                }
            }
            IOUtils.writeDataJSON(field.toString(), "field");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
