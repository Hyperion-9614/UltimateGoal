package com.hyperion.dashboard;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.uiobject.FieldEdit;

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
            Constants.HOST_IP = InetAddress.getLocalHost().getHostAddress();
            Constants.write();
            configuration.setHostname(Constants.HOST_IP);
            configuration.setPort(Constants.PORT);
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
                Utils.printSocketLog(type, "SERVER", address + " connected");

                client.sendEvent("constantsUpdated", Utils.readDataJSON("constants"));
                Utils.printSocketLog("SERVER", type, "constantsUpdated");

                client.sendEvent("fieldEdited", readDashboardAsFieldEdits(Utils.readDataJSON("field")).toString());
                Utils.printSocketLog("SERVER", type, "fieldEdited");
            });

            server.addDisconnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                if (rcClient != null && address.equals("192.168.49.1")) {
                    rcClient = null;
                    Utils.printSocketLog("RC", "SERVER", "disconnected");
                } else {
                    Utils.printSocketLog("UI", "SERVER", "disconnected");
                    dashboardClients.remove(client);
                }
            });

            server.addEventListener("fieldEdited", String.class, (client, data, ackRequest) -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                String type = (rcClient != null && address.equals("192.168.49.1")) ? "RC" : "UI";
                Utils.printSocketLog(type, "SERVER", "fieldEdited");

                for (SocketIOClient dashboardClient : dashboardClients) {
                    if (!dashboardClient.getRemoteAddress().equals(client.getRemoteAddress())) {
                        dashboardClient.sendEvent("fieldEdited", data);
                        Utils.printSocketLog("SERVER", "UI", "fieldEdited");
                    }
                }
                if (rcClient != null) {
                    rcClient.sendEvent("fieldEdited", data);
                    Utils.printSocketLog("SERVER", "RC", "fieldEdited");
                }

                writeEditsToFieldJSON(data);
            });

            server.addEventListener("opModeEnded", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("opModeEnded", data);
                    Utils.printSocketLog("SERVER", "UI", "opModeEnded");
                }
            });

            server.addEventListener("unimetryUpdated", String.class, (client, data, ackRequest) -> {
                Utils.printSocketLog("RC", "SERVER", "unimetryUpdated");

                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("unimetryUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "unimetryUpdated");
                }
            });

            server.addEventListener("constantsUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    if (!dashboardClient.getRemoteAddress().equals(client.getRemoteAddress())) {
                        dashboardClient.sendEvent("constantsUpdated", data);
                        Utils.printSocketLog("SERVER", "UI", "constantsUpdated");
                    }
                }
                if (rcClient != null) {
                    rcClient.sendEvent("constantsUpdated", data);
                    Utils.printSocketLog("SERVER", "RC", "constantsUpdated");
                }

                Constants.read(new JSONObject(data));
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
            JSONObject field = new JSONObject(Utils.readDataJSON("field"));
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
            Utils.writeDataJSON(field.toString(), "field");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
